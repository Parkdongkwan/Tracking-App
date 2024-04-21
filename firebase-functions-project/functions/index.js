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

exports.resetWeeklyData = functions.pubsub.schedule("0 12 * * SUN")
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

exports.compareDailyIntakeWithGoal = functions.pubsub.schedule("0 0 * * *").
    timeZone("Asia/Kuala_Lumpur")
    .onRun((context) => {
      const db = admin.database();
      const usersRef = db.ref("users");
      usersRef.once("value", (snapshot) => {
        snapshot.forEach((childSnapshot) => {
          const userId = childSnapshot.key;
          const userRef = db.ref(`users/${userId}`);
          userRef.once("value", (userSnapshot) => {
            const userData = userSnapshot.val();
            const userCalorieGoal = userData.calorieGoal;
            // Calculate total calories from daily intake
            let totalCalories = 0;
            if (userData.dailyIntakes) {
              Object.values(userData.dailyIntakes).forEach((dailyIntake) => {
                totalCalories += dailyIntake.calories;
              });
            }
            // Compare total calories with goal
            const difference = totalCalories - userCalorieGoal;
            // Send notification based on the comparison result
            if (difference > 100) {
              // Notification for higher calorie intake
              sendNotification(userId, "Higher Calorie Intake", `Your da
              ily calorie inta
              ke is higher than your goal by ${difference} calories.`);
            } else if (difference < -100) {
              // Notification for lower calorie intake
              sendNotification(userId, "Lower Calorie Intake", `You are near y
              our daily calorie intake goal.`);
            }
          });
        });
      });
      return null;
    });
/**
 * Sends a notification to a user.
 * @param {string} userId - The ID of the user to send the notification to.
 * @param {string} title - The title of the notification.
 * @param {string} body - The body content of the notification.
 */
function sendNotification(userId, title, body) {
  const message = {
    notification: {
      title: title,
      body: body,
    },
    token: "USER_DEVICE_TOKEN", // Replace with the user's device token
  };
  admin.messaging().send(message)
      .then((response) => {
        console.log(`Notification sent to user ${userId}:`, response);
      })
      .catch((error) => {
        console.error(`Error sending notification to user ${userId}:`, error);
      });
}
