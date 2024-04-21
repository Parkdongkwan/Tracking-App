const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.resetDailyIntake = functions.pubsub.schedule("0 0 * * *").
    timeZone("Asia/Kuala_Lumpur").
    onRun((context) => {
    // Get a reference to your Firebase database
      const db = admin.database();

      const usersRef = db.ref("users");
      usersRef.once("value", (snapshot) => {
        snapshot.forEach((childSnapshot) => {
          const userId = childSnapshot.key;
          const dailyIntakesRef = db.ref(`users/${userId}/dailyIntakes`);
          // Delete everything in dailyIntakes
          dailyIntakesRef.remove()
              .then(() => {
                console.log(`Daily intake data for user
                 ${userId} deleted successfully.`);
              })
              .catch((error) => {
                console.error(`Error deleting daily 
                intake data for user ${userId}:`, error);
              });
        });
      });

      console.log("Daily intake data reset successfully.");

      return null;
    });

exports.resetWeeklyData = functions.pubsub.schedule("0 12 * * MON")
    .timeZone("Asia/Kuala_Lumpur")
    .onRun((context) => {
      // Get a reference to your Firebase database
      const db = admin.database();

      const usersRef = db.ref("users");
      usersRef.once("value", (snapshot) => {
        snapshot.forEach((childSnapshot) => {
          const userId = childSnapshot.key;
          const weeklyIntakesRef = db.ref(`users/${userId}/weeklyIntake`);
          // Delete everything in dailyIntakes
          weeklyIntakesRef.remove()
              .then(() => {
                console.log(`Weekly intake data for user
                 ${userId} deleted successfully.`);
              })
              .catch((error) => {
                console.error(`Error deleting weekly 
                intake data for user ${userId}:`, error);
              });
        });
      });

      console.log("Weekly intake data reset successfully.");

      return null;
    });
