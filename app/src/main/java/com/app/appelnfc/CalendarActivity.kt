package com.app.appelnfc

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Faire un appel réseau au démarrage de l'activité
        makeNetworkRequest("https://www.emploisdutemps.uha.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?data=468b193a5ff10ceca34136a7e8f67c5ba5f10b982f9b914f8b3df9a16d82f4932a2c262ab3ba48506729f6560ae33af6fa8513c753526e332bda1edc491dcfab,1")

    }

    fun makeNetworkRequest(url: String) {
        Thread {
            try {
                val urlObject = URL(url)
                val httpURLConnection = urlObject.openConnection() as HttpURLConnection
                httpURLConnection.requestMethod = "GET"
                httpURLConnection.connect()

                val inputStream = httpURLConnection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))

                val stringBuilder = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }

                val response = stringBuilder.toString()

                // Log the response
                runOnUiThread {
                    Log.i("NFC", "Response: $response")
                }

                val events = extractEvents(response) // List of events from network request


                runOnUiThread {
                    val eventAdapter = EventAdapter(this, events)
                    val listView = findViewById<ListView>(R.id.listView)
                    listView.adapter = eventAdapter
                    Log.i("NFC", "Extracted Events: $listView")
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("NFC", "Error fetching data", e)
                }
            }
        }.start()
    }

    fun extractEvents(input: String): List<Event> {
        val events = mutableListOf<Event>()

        // Ajustement de l'expression régulière pour prendre en compte les sauts de ligne
        val pattern = Pattern.compile(
            "BEGIN:VEVENT.*?SUMMARY:(.*?)LOCATION:(.*?)DESCRIPTION:(.*?)DTSTART:(.*?)DTEND:(.*?)UID:",
            Pattern.DOTALL
        )

        val matcher = pattern.matcher(input)

        while (matcher.find()) {
            val summary = matcher.group(1)?.trim()
            val location = matcher.group(2)?.trim()

            // Supprimer les sauts de ligne de la description
            val description = matcher.group(3)?.replace("\n", " ")?.trim()

            val dtStart = matcher.group(4)?.trim()
            val dtEnd = matcher.group(5)?.trim()

            Log.i("NFC", "Extracted event: $summary, $location, $description, $dtStart, $dtEnd")

            events.add(Event(
                summary = summary ?: "N/A",
                location = location ?: "N/A",
                description = description ?: "N/A",
                dtStart = convertDate(dtStart ?: "N/A"),
                dtEnd = convertDate(dtEnd ?: "N/A")
            ))

        }
        return events
    }


    fun convertDate(iCalDate: String): String {
        val inputFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("yyyy/MM/dd-HH'h'", Locale.US)
        val date = inputFormat.parse(iCalDate)
        return if (date != null) outputFormat.format(date) else iCalDate
    }

}


