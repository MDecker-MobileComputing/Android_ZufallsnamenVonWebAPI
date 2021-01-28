package de.mide.zufallsnamenvonwebapi;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * App ruft von einer Web-API zufällige Personen-Namen im JSON-Format ab.
 * Verwendete Web-API: <a href="https://randomuser.me">https://randomuser.me</a> .
 * Zugehöriger Eintrag im API-Verzeichnis von <i>ProgrammableWeb</i>:
 * <a href="http://www.programmableweb.com/api/randomuser">http://www.programmableweb.com/api/randomuser</a>
 * <br><br>
 *
 * This file is licensed under the terms of the BSD 3-Clause License.
 */
public class MainActivity extends Activity {
	
	public static final String TAG4LOGGING = "ZufallsNamenVonWebAPI";
	
	/** Button mit dem der Web-Request gestartet wird. */
	protected Button _startButton = null;
	
	/** 
     * TextView zur Anzeige des Ergebnisses des Web-Requests (also die zufälligen Namen)
	 * oder einer Fehlermeldung. 
	 */
	protected TextView _ergebnisTextView = null;	
	

	/**
	 * Lifecycle-Methode; Layout für UI laden und Referenzen auf UI-Elemente holen.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
				
		_startButton      = findViewById( R.id.starteWebRequestButton );
		_ergebnisTextView = findViewById( R.id.ergebnisTextView       );
	}
		
	
	/**
	 * Event-Handler für Start-Button, wird in Layout-Datei
	 * mit Attribut <i>"android:onClick"</i> zugewiesen.
	 */
	public void onStartButtonBetaetigt(View view) {
		
		_startButton.setEnabled(false); // Button deaktivieren während ein HTTP-Request läuft
		
		_ergebnisTextView.setText("Starte HTTP-Request ...");

		
	    // Hintergrund-Thread mit HTTP-Request starten
		MeinHintergrundThread mht = new MeinHintergrundThread();
		mht.start();
		// als Einzeiler: new MeinHintergrundThread().start();
	}
	
	
	/** 
	 * In dieser Methode wird der HTTP-Request zur Web-API durchgeführt.
	 * Achtung: Diese Methode darf nicht im Main-Thread ausgeführt werden,
	 * weil ein Internet-Zugriff länger dauern kann (mehrere Sekunden oder Minuten),
	 * so dass die App wegen <i>"Application Not Responding" (ANR)</i> ggf. 
	 * vom Nutzer abgebrochen würde.
	 * 
	 * @return String mit JSON-Dokument, das als Antwort von der Web-API 
	 *         zurückgeliefert wurde.
	 */
	protected String holeDatenVonWebAPI() throws Exception {

        URL url                                = null;
        HttpURLConnection conn                 = null;
        String            httpErgebnisDokument = "";


        url  = new URL("https://api.randomuser.me/?results=3&gender=male&format=json");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET"); // Eigentlich nicht nötig, weil "GET" Default-Wert ist.

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

            String errorMessage = "HTTP-Fehler: " + conn.getResponseMessage();
            throw new Exception( errorMessage );

        } else {

            InputStream is        = conn.getInputStream();
            InputStreamReader ris = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(ris);

            // JSON-Dokument zeilenweise einlesen
            String zeile = "";
            while ( (zeile = reader.readLine()) != null) {

                httpErgebnisDokument += zeile;
            }
        }

        Log.i(TAG4LOGGING, "JSON-String erhalten: " + httpErgebnisDokument);

        return httpErgebnisDokument;
	}	
	
	
	/**
	 * Parsen des JSON-Dokuments <i>jsonString</i>, das von der Web-API 
	 * zurückgeliefert wurde.<br><br>

	 * Es wird der in Android seit API-Level eingebaute JSON-Parser verwendet:
	 * <a href="http://developer.android.com/reference/org/json/JSONObject.html">
	 * http://developer.android.com/reference/org/json/JSONObject.html</a>.
     * <br><br>
     *
	 * Es werden <b>nicht</b> alle in dem JSON-Dokument enthaltenen Informationen
	 * ausgewertet.
	 * 
	 * @param jsonString JSON-Dokument, das die Web-API zurückgeliefert hat.
	 * 
	 * @return String mit Ergebnis (Personen-Namen), zur Anzeige auf UI.
	 * 
	 * @throws JSONException wenn das JSON-Dokument fehlerhaft ist. 
	 */	
	protected String parseJSON(String jsonString) throws JSONException {
				
		if (jsonString == null || jsonString.trim().length() == 0) {

			return "Leeres JSON-Objekt von Web-API erhalten.";
		}
		
		// Für den Ergebnis-String wird ein StringBuffer-Objekt verwendet,
		// weil hiermit String-Konkatenationen effizienter sind.
		StringBuffer sbuf = new StringBuffer();		
		
		
		// Eigentliches Parsen durch Aufruf des Konstruktors der Klasse JSONObject.
		// Wenn das JSON-Dokument einen syntaktischen Fehler enthält, dann wirft
		// der Konstruktor eine Exception.
		JSONObject jsonObject = new JSONObject(jsonString); 
		
		
		// Das JSON-Objekt enthält auf oberster Ebene nur das Attribut <i>results</i>,
		// welches einen Array mit den einzelnen Personen-Datensätzen enthält.
		// Da wir den URL-Parameter "results=3" definiert haben, sollten drei
		// Datensätze zurückgeliefert werden.
		JSONArray arrayResults = jsonObject.getJSONArray( "results" );
		int anzPersonen = arrayResults.length();
		
		sbuf.append("Anzahl Personen-Datensätze von Web-API erhalten: ").append(anzPersonen).append("\n\n");
		
		
		// Einzelne Personen-Unterobjekte aus JSON-Objekt holen
		for (int i = 0; i < anzPersonen; i++) {
			
			JSONObject resultObject = (JSONObject)arrayResults.get(i);
			if (resultObject == null) {

				return "Fehler beim JSON-Parser: User-Objekt mit Index " + i + " war null.";
			}
			
			String userString = parseUserObjekt( resultObject );
			
			sbuf.append( userString ).append("\n");
		}

		return sbuf.toString();
	}

	
	/**
	 * JSON-Objekt mit einem Datensatz auswerten.
	 * 
	 * @param resultObject JSON-Objekt mit einem Personen-Datensatz
	 * 
	 * @return Vor- und Nachname als String.
	 *  
	 * @throws JSONException Fehler in JSON-Datei
	 */
	protected String parseUserObjekt(JSONObject resultObject) throws JSONException {
		
		JSONObject nameObject = resultObject.getJSONObject( "name" );
		if (nameObject == null) {

			return "Fehler: ResultObject enthielt nicht das Attribut \"name\"";
		}
		
		String vornameString  = nameObject.getString( "first" );
		String nachnameString = nameObject.getString( "last"  );
		// ... nameObject.getString( "title" )
		
		Log.i(TAG4LOGGING, "Name aus JSON-Objekt: " + vornameString + " " + nachnameString);
		
				
		vornameString  = ersterBuchstabeGross( vornameString  );
		nachnameString = ersterBuchstabeGross( nachnameString );
		
		return vornameString + " " + nachnameString;
	}
	
	
	/**
	 * Methode stellt sicher, dass der Eingabe-String <i>name</i> mit einem Großbuchstaben beginnt.
	 * 
	 * @param name String, dessen erster Buchstabe in einen Großbuchstaben umgewandelt werden soll.
	 * 
	 * @return String, der mit 
	 */
	@SuppressLint("DefaultLocale")
	protected String ersterBuchstabeGross(String name) {
		
		if (name == null) {

			return "";
		}

		name = name.trim(); // Leerzeichen am Anfange & Ende entfernen
		
		if (name.length() == 0) {

			return "";
		}
		
		// Eigentliche Umwandlung
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}
	
	
	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */	
	
	/**
	 * Zugriff auf Web-API (Internet-Zugriff) wird in
	 * eigenen Thread ausgelagert, damit der Main-Thread
	 * nicht blockiert wird.
	 */
	protected class MeinHintergrundThread extends Thread {

		/** Konstruktor für "Self-Starting Thread". */
		/*
		protected MeinHintergrundThread() {
			start();
		}
		*/
		
		/**
		 * Der Inhalt in der überschriebenen <i>run()</i>-Methode
		 * wird in einem Hintergrund-Thread ausgeführt.
		 */
		@Override
		public void run() {
			
			try {

				String jsonDocument = holeDatenVonWebAPI();
				
				String ergString = parseJSON(jsonDocument);
				
				ergbnisDarstellen( "Ergebnis von Web-Request:\n\n" + ergString );
			}
			catch (Exception ex) {

				ergbnisDarstellen( "Exception aufgetreten: " + ex.getMessage() );
			}			
		}

		
		/**
		 * Methode um Ergebnis-String in TextView darzustellen. Da
		 * es sich hierbei um einen UI-Zugriff handelt, müssen
		 * wir durch Verwendung <i>post()</i>-Methode dafür sorgen, dass die
		 * UI-Zugriffe aus dem Main-Thread heraus durchgeführt werden.
		 * Der Start-Button wird auch wieder aktiviert (er wurde beim
		 * Beginn des Lade-Vorgangs deaktiviert).  
		 * 
		 * @param ergebnisStr Nachricht, die in TextView-Element dargestellt werden soll.
		 */
		protected void ergbnisDarstellen(String ergebnisStr) {
			
			final String finalString = ergebnisStr;
			
			_startButton.post( new Runnable() { // wir könnten auch die post()-Methode des TextView-Elements verwenden
				@Override
				public void run() {

					_startButton.setEnabled(true);
					
					_ergebnisTextView.setText(finalString);
				}
			});									
		}
		
	};
	
	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */		
	
};
