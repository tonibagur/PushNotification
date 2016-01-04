package com.plugin.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Notification;
import android.app.Notification.Style;
import android.app.Notification.BigTextStyle;
import android.app.Notification.InboxStyle;
//import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import 	java.io.PrintWriter;
import java.io.Writer;
import java.io.StringWriter;
import java.util.Random;

/*
 * Implementation of GCMBroadcastReceiver that hard-wires the intent service to be 
 * com.plugin.gcm.GcmntentService, instead of your_package.GcmIntentService 
 */
public class CordovaGCMBroadcastReceiver extends WakefulBroadcastReceiver {
	private static final String TAG = "GcmIntentService";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "onHandleIntent - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);

		if (extras != null) {
			try {
				if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
					JSONObject json = new JSONObject();

					json.put("event", "error");
					json.put("message", extras.toString());
					PushPlugin.sendJavascript(json);
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
					JSONObject json = new JSONObject();
					json.put("event", "deleted");
					json.put("message", extras.toString());
					PushPlugin.sendJavascript(json);
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
					// if we are in the foreground, just surface the payload, else post it to the statusbar
					if (PushPlugin.isInForeground()) {
						extras.putBoolean("foreground", true);
						PushPlugin.sendExtras(extras);
					} else {
						extras.putBoolean("foreground", false);

						// Send a notification if there is a message
						if (extras.getString("message") != null && extras.getString("message").length() != 0) {
							createNotification(context, extras);
						}
					}
				}
			} catch (JSONException exception) {
				Log.d(TAG, "JSON Exception was had!");
			}
		}
	}

	public void createNotification(Context context, Bundle extras) {
		Log.i("PushPlugin", "createNotification");
		int notId = 51626974; // Id of Notification's app ("Qbit" to Hexadecimal).
		
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(context);

		Intent notificationIntent = new Intent(context, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);
    	PendingIntent contentIntent = PendingIntent.getActivity(context, notId, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	String error_str="";
    	// Set Notification settings
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException ignore) {
			}
		}

		String soundName = extras.getString("sound");
		if (soundName != null) {
			Resources r = context.getResources();
			int resourceId = r.getIdentifier(soundName, "raw", context.getPackageName());
			Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resourceId);
			//mBuilder.setSound(soundUri);
			defaults &= ~Notification.DEFAULT_SOUND;
			//mBuilder.setDefaults(defaults);
		}

		// Creates Notification style
		Notification.InboxStyle notiStyle = new Notification.InboxStyle();
        notiStyle.setBigContentTitle("Qbit");

        // Get messages of response
        int nSenders = 1;
		String messages = extras.getString("messagesNotRead");
		int nMessages = 0;

		int bucle = 22;

		String parseJson ="";
		if (messages == null) {
			notiStyle.addLine("<missing message content>");				
		}else {
			try{
				// Parse response data
				JSONArray messagesArray = new JSONArray(messages);
				// Number of messages unread.
				JSONObject row = (JSONObject) messagesArray.get(0);
				//nMessages = messagesArray.length();
			    nMessages = Integer.parseInt((String) row.get("num"));
				nSenders = Integer.parseInt(extras.getString("nSenders"));
				// Max messages displayed in InboxNotification.
				int maxDisplayedMessages = 5;
				int maxIter = if(nMessages > 5) ? 5 : nMessages;
				
				// Fill Notification with messages
				for(int i = 0; i < maxIter; i++){
					JSONObject message = (JSONObject) messagesArray.get(i);
					// If message is from unknown sender takes phonenumber("username").
					String sender = (String) message.get("contact_name");
					if(sender.equals("") || sender == null){
						sender = (String) message.get("username");
					}					
					String content = (String) message.get("contenido");
					parseJson += content;
					notiStyle.addLine(sender + ": " +content);
				}
				
				// If there are more messages than maximum displayed Notification will have a summarytext.
				if(nMessages > maxDisplayedMessages){
					if(nSenders == 1){
						notiStyle.setSummaryText("+ "+ (nMessages - maxDisplayedMessages) +" new messages");
					}else {
						notiStyle.setSummaryText("+ "+ (nMessages - maxDisplayedMessages) +" new messages from " + nSenders + " chats");
					}
				}
			} catch (JSONException e){
				Writer writer = new StringWriter();
				PrintWriter printWriter = new PrintWriter(writer);
				e.printStackTrace(printWriter);
				error_str = writer.toString();
			}
		}
  		
  		String text = "You have " + nMessages + " new messages";
  		if(nSenders > 1){
  			text = "You have " + nMessages + " new messages from " + nSenders + " chats";
  		}
		Notification notification = new Notification.Builder(context)
		    .setDefaults(defaults)
		    //.setSound(soundUri)
		    .setSmallIcon(getSmallIcon(context, extras))
		    .setWhen(System.currentTimeMillis())
		    .setContentTitle("Qbit")
		    .setContentText(text)
		    .setTicker(extras.getString("title"))
		    .setContentIntent(contentIntent)
		    .setColor(getColor(extras))
		    .setNumber(Integer.parseInt(extras.getString("msgcnt")))
		    .setAutoCancel(true)
		    .setStyle(notiStyle)
		    .build(); 

		final int largeIcon = getLargeIcon(context, extras);
		Log.e(TAG, "largeIcon: " +largeIcon);
		if (largeIcon > -1) {
			notification.contentView.setImageViewResource(android.R.id.icon, largeIcon);
		}

		mNotificationManager.notify(appName, notId, notification);
	}

	private static String getAppName(Context context) {
		CharSequence appName =
				context
						.getPackageManager()
						.getApplicationLabel(context.getApplicationInfo());

		return (String) appName;
	}

	private int getColor(Bundle extras) {
		int theColor = 0; // default, transparent
		final String passedColor = extras.getString("color"); // something like "#FFFF0000", or "red"
		if (passedColor != null) {
			try {
				theColor = Color.parseColor(passedColor);
			} catch (IllegalArgumentException ignore) {}
			}
		return theColor;
	}

	private int getSmallIcon(Context context, Bundle extras) {

		int icon = -1;

		// first try an iconname possible passed in the server payload
		final String iconNameFromServer = extras.getString("smallIcon");
		if (iconNameFromServer != null) {
			icon = getIconValue(context.getPackageName(), iconNameFromServer);
		}

		// try a custom included icon in our bundle named ic_stat_notify(.png)
		if (icon == -1) {
			icon = getIconValue(context.getPackageName(), "ic_stat_notify");
		}

		// fall back to the regular app icon
		if (icon == -1) {
			icon = context.getApplicationInfo().icon;
		}

		return icon;
	}

	private int getLargeIcon(Context context, Bundle extras) {

		int icon = -1;

		// first try an iconname possible passed in the server payload
		final String iconNameFromServer = extras.getString("largeIcon");
		if (iconNameFromServer != null) {
			icon = getIconValue(context.getPackageName(), iconNameFromServer);
		}

		// try a custom included icon in our bundle named ic_stat_notify(.png)
		if (icon == -1) {
			icon = getIconValue(context.getPackageName(), "ic_notify");
		}

		// fall back to the regular app icon
		if (icon == -1) {
			icon = context.getApplicationInfo().icon;
		}

		return icon;
	}

	private int getIconValue(String className, String iconName) {
		try {
			Class<?> clazz  = Class.forName(className + ".R$drawable");
			return (Integer) clazz.getDeclaredField(iconName).get(Integer.class);
		} catch (Exception ignore) {}
		return -1;
	}
}
