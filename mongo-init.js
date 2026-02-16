db = db.getSiblingDB("admin");

db.createUser({
  user: "mongo_user",
  pwd: "mongo_pass",
  roles: [
    { role: "readWrite", db: "payments_db" },
    { role: "dbAdmin", db: "payments_db" }
  ]
});