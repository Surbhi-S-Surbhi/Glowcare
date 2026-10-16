
require("dotenv").config();

const express = require("express");
const cors = require("cors");
const crypto = require("crypto");
const { OAuth2Client } = require("google-auth-library");
const Razorpay = require("razorpay");
const { Pool } = require("pg");

const app = express();
const PORT = process.env.PORT || 3000;

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const db = new Pool({
  connectionString: process.env.DATABASE_URL,
});

const razorpay = new Razorpay({
  key_id: process.env.RAZORPAY_KEY_ID,
  key_secret: process.env.RAZORPAY_KEY_SECRET,
});

const plans = {
  monthly: process.env.RAZORPAY_MONTHLY_PLAN_ID,
  yearly: process.env.RAZORPAY_YEARLY_PLAN_ID,
};

// Webhook needs the original, unmodified request body.
app.post(
  "/api/razorpay/webhook",
  express.raw({ type: "application/json" }),
  async (req, res) => {
    try {
      const secret = process.env.RAZORPAY_WEBHOOK_SECRET;
      const signature = req.headers["x-razorpay-signature"];

      if (!secret || !signature || !Buffer.isBuffer(req.body)) {
        return res.status(400).send("Webhook configuration missing");
      }

      const expected = crypto
        .createHmac("sha256", secret)
        .update(req.body)
        .digest("hex");

      const a = Buffer.from(expected);
      const b = Buffer.from(signature);

      if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
        return res.status(400).send("Invalid webhook signature");
      }

      const event = JSON.parse(req.body.toString("utf8"));
      const entity =
        event.payload?.subscription?.entity ||
        event.payload?.payment?.entity;

      const subscriptionId =
        entity?.id || entity?.subscription_id;

      if (!subscriptionId) {
        return res.status(200).send("No subscription to update");
      }

      const eventStatus = {
        "subscription.activated": "active",
        "subscription.charged": "active",
        "subscription.cancelled": "cancelled",
        "subscription.halted": "halted",
        "subscription.completed": "expired",
        "subscription.expired": "expired",
      }[event.event];

      if (eventStatus) {
        await db.query(
          `UPDATE subscriptions
           SET status = $1,
               current_period_end = CASE
                 WHEN $1 IN ('cancelled','halted','expired')
                   THEN current_period_end
                 ELSE COALESCE(
                   $2::timestamptz,
                   current_period_end
                 )
               END,
               updated_at = NOW()
           WHERE razorpay_subscription_id = $3`,
          [
            eventStatus,
            entity?.current_end
              ? new Date(entity.current_end * 1000).toISOString()
              : null,
            subscriptionId,
          ]
        );
      }

      return res.status(200).send("Webhook processed");
    } catch (error) {
      console.error("Webhook error:", error.message);
      return res.status(500).send("Webhook processing failed");
    }
  }
);

app.use(cors());
app.use(express.json());

async function authenticateGoogle(req, res, next) {
  try {
    const header = req.headers.authorization || "";
    const match = header.match(/^Bearer (.+)$/);

    if (!match) {
      return res.status(401).json({ error: "Google token required" });
    }

    const ticket = await googleClient.verifyIdToken({
      idToken: match[1],
      audience: process.env.GOOGLE_CLIENT_ID,
    });

    const payload = ticket.getPayload();

    if (!payload?.sub) {
      return res.status(401).json({ error: "Invalid Google token" });
    }

    req.user = {
      googleSub: payload.sub,
      email: payload.email || null,
      name: payload.name || null,
    };

    next();
  } catch (error) {
    console.error("Google verification error:", error.message);
    return res.status(401).json({ error: "Google authentication failed" });
  }
}

function mapRazorpayStatus(status) {
  const allowed = [
    "active",
    "cancelled",
    "expired",
    "halted",
    "pending",
  ];

  if (status === "active") return "active";
  if (status === "cancelled") return "cancelled";
  if (status === "completed" || status === "expired") return "expired";
  if (status === "halted") return "halted";

  return allowed.includes(status) ? status : "pending";
}

app.get("/", (req, res) => {
  res.json({ message: "GlowCare backend is running" });
});

// Verify a Google ID token sent by the Android app.
app.post("/api/auth/google", authenticateGoogle, (req, res) => {
  res.json({
    verified: true,
    googleSub: req.user.googleSub,
    email: req.user.email,
    name: req.user.name,
  });
});

// Create a Razorpay subscription for the signed-in Google account.
app.post("/api/subscriptions/create", authenticateGoogle, async (req, res) => {
  try {
    const { plan } = req.body;

    if (!["monthly", "yearly"].includes(plan) || !plans[plan]) {
      return res.status(400).json({ error: "Invalid subscription plan" });
    }

    const existing = await db.query(
      `SELECT status FROM subscriptions WHERE google_sub = $1`,
      [req.user.googleSub]
    );

    if (existing.rows[0]?.status === "active") {
      return res.status(409).json({
        error: "An active subscription already exists",
      });
    }

    const subscription = await razorpay.subscriptions.create({
      plan_id: plans[plan],
      total_count: plan === "monthly" ? 120 : 10,
      quantity: 1,
      customer_notify: 1,
      notes: {
        google_sub: req.user.googleSub,
        plan,
      },
    });

    await db.query(
      `INSERT INTO subscriptions
        (google_sub, email, razorpay_subscription_id, plan, status)
       VALUES ($1, $2, $3, $4, 'pending')
       ON CONFLICT (google_sub) DO UPDATE SET
         email = EXCLUDED.email,
         razorpay_subscription_id = EXCLUDED.razorpay_subscription_id,
         plan = EXCLUDED.plan,
         status = 'pending',
         current_period_end = NULL,
         updated_at = NOW()`,
      [
        req.user.googleSub,
        req.user.email,
        subscription.id,
        plan,
      ]
    );

    res.json({
      subscriptionId: subscription.id,
      razorpayKeyId: process.env.RAZORPAY_KEY_ID,
      plan,
      status: "pending",
    });
  } catch (error) {
    console.error("Create subscription error:", error.message);
    res.status(500).json({ error: "Could not create subscription" });
  }
});

// Verify the checkout signature, then query Razorpay for its real status.
app.post("/api/subscriptions/verify", authenticateGoogle, async (req, res) => {
  try {
    const {
      razorpay_payment_id: paymentId,
      razorpay_subscription_id: subscriptionId,
      razorpay_signature: signature,
    } = req.body;

    if (!paymentId || !subscriptionId || !signature) {
      return res.status(400).json({ error: "Missing checkout fields" });
    }

    const record = await db.query(
      `SELECT * FROM subscriptions
       WHERE google_sub = $1 AND razorpay_subscription_id = $2`,
      [req.user.googleSub, subscriptionId]
    );

    if (!record.rows.length) {
      return res.status(403).json({ error: "Subscription does not belong to this account" });
    }

    const expected = crypto
      .createHmac("sha256", process.env.RAZORPAY_KEY_SECRET)
      .update(`${paymentId}|${subscriptionId}`)
      .digest("hex");

    const a = Buffer.from(expected);
    const b = Buffer.from(signature);

    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
      return res.status(400).json({ error: "Invalid payment signature" });
    }

    const remote = await razorpay.subscriptions.fetch(subscriptionId);
    const status = mapRazorpayStatus(remote.status);

    await db.query(
      `UPDATE subscriptions
       SET status = $1,
           current_period_end = $2,
           updated_at = NOW()
       WHERE google_sub = $3 AND razorpay_subscription_id = $4`,
      [
        status,
        remote.current_end
          ? new Date(remote.current_end * 1000).toISOString()
          : null,
        req.user.googleSub,
        subscriptionId,
      ]
    );

    res.json({
      verified: true,
      status,
      premium: status === "active",
    });
  } catch (error) {
    console.error("Verify subscription error:", error.message);
    res.status(500).json({ error: "Could not verify subscription" });
  }
});

// Restore the subscription for the currently signed-in Google account.
app.get("/api/subscriptions/me", authenticateGoogle, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT razorpay_subscription_id, plan, status, current_period_end
       FROM subscriptions WHERE google_sub = $1`,
      [req.user.googleSub]
    );

    if (!result.rows.length) {
      return res.json({ premium: false, subscription: null });
    }

    const row = result.rows[0];

    if (row.razorpay_subscription_id) {
      try {
        const remote = await razorpay.subscriptions.fetch(
          row.razorpay_subscription_id
        );

        const status = mapRazorpayStatus(remote.status);

        await db.query(
          `UPDATE subscriptions
           SET status = $1, current_period_end = $2, updated_at = NOW()
           WHERE google_sub = $3`,
          [
            status,
            remote.current_end
              ? new Date(remote.current_end * 1000).toISOString()
              : null,
            req.user.googleSub,
          ]
        );

        row.status = status;
        row.current_period_end = remote.current_end
          ? new Date(remote.current_end * 1000).toISOString()
          : null;
      } catch (error) {
        console.error("Razorpay status fetch error:", error.message);
        return res.status(503).json({
          error: "Unable to verify subscription status right now",
        });
      }
    }

    res.json({
      premium: row.status === "active",
      subscription: row,
    });
  } catch (error) {
    console.error("Restore subscription error:", error.message);
    res.status(500).json({ error: "Could not restore subscription" });
  }
});

async function start() {
  await db.query("SELECT 1");
  app.listen(PORT, () => {
    console.log(`GlowCare backend running on port ${PORT}`);
  });
}

start().catch((error) => {
  console.error("Backend startup failed:", error.message);
  process.exit(1);
});

