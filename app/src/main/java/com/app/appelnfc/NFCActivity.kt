package com.app.appelnfc

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord.createMime
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.xmlpull.v1.XmlPullParser

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class NFCActivity : AppCompatActivity() {

    private var nfcAdapter1: NfcAdapter? = null
    val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    var pendingIntent: PendingIntent? = null

    private val tableDataList = mutableListOf<TableData>()
    private lateinit var adapter: ArrayAdapter<TableData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        nfcAdapter1 = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter1== null) {
            Toast.makeText(this, "Ce dispositif ne supporte pas NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!nfcAdapter1!!.isEnabled) {
            Toast.makeText(this, "NFC est désactivé.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "NFC est activé.", Toast.LENGTH_SHORT).show()
        }

        adapter = object : ArrayAdapter<TableData>(this, R.layout.row_layout, tableDataList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.row_layout, parent, false)

                val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
                checkBox.isChecked = getItem(position)?.isChecked ?: false
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    getItem(position)?.isChecked = isChecked
                }

                val textViewNom = view.findViewById<TextView>(R.id.textViewNom)
                textViewNom.text = getItem(position)?.Nom

                val textViewPrenom = view.findViewById<TextView>(R.id.textViewPrenom)
                textViewPrenom.text = getItem(position)?.Prenom

                val textViewINE = view.findViewById<TextView>(R.id.textViewINE)
                textViewINE.text = getItem(position)?.INE

                return view
            }
        }
        val listView: ListView = findViewById(R.id.listView)
        listView.adapter = adapter
        // modification titre
        val nameCoursTextView = findViewById<TextView>(R.id.NameCours)
        nameCoursTextView.text = "MDS"

        val startHoursTextView = findViewById<TextView>(R.id.StartHours)
        startHoursTextView.text = "9h"

        val endHoursTextView = findViewById<TextView>(R.id.EndHours)
        endHoursTextView.text = "19h"

        setForeground()
    }

    private fun setForeground() {
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onResume() {
        super.onResume()

        nfcAdapter?.enableReaderMode(this, this::onTagDiscovered, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
        //nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        nfcAdapter?.disableReaderMode(this)
        //nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun onTagDiscovered(tag: Tag) {
        val cardSerialNumber = tag.id.toList().joinToString(":") {
            it.toUByte().toString(16).padStart(2, '0')
        }
        println(cardSerialNumber)
        Log.i("NFC", cardSerialNumber)

        val student = getStudentInfoByCardSerial(cardSerialNumber)

        runOnUiThread {
            if (student != null) {
                addTableRow(student.nom, student.prenom,student.numEtudiant,cardSerialNumber)
            }
        }
        if (student != null) {
            val studentInfo = "Nom: ${student.nom}, Prénom: ${student.prenom}, Numéro Étudiant: ${student.numEtudiant}"
            Toast.makeText(this, studentInfo, Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(this, "Étudiant non trouvé", Toast.LENGTH_LONG).show()
        }
    }

    private fun addTableRow(Nom: String, Prenom: String,INE: String ,SerialNbr:String) {
        val existsSerial = tableDataList.any { it.SerialNbr == SerialNbr }
        val existsINE = tableDataList.any { it.INE == INE }
       // if (!existsSerial and !existsINE) {
            val tableData = TableData(Nom, Prenom, INE, SerialNbr)
            tableDataList.add(tableData)
            adapter.notifyDataSetChanged()
        //}
    }

    private fun getStudentInfoByCardSerial(cardSerial: String): Student? {
        val parser = resources.getXml(R.xml.students_map)
        var eventType = parser.eventType
        var currentStudent: Student? = null
        var currentNumSerie: String? = null
        var currentNom: String? = null
        var currentPrenom: String? = null
        var currentNumEtudiant: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "numSerie" -> currentNumSerie = parser.nextText()
                        "nom" -> currentNom = parser.nextText()
                        "prenom" -> currentPrenom = parser.nextText()
                        "numEtudiant" -> currentNumEtudiant = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "etudiant") {
                        if (currentNumSerie?.lowercase(Locale.ROOT) == cardSerial.lowercase(Locale.ROOT)) {
                            currentStudent = Student(currentNumSerie, currentNom ?: "", currentPrenom ?: "", currentNumEtudiant ?: "")
                            break
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        Log.i("NFC", currentStudent.toString())
        return currentStudent
    }
    private fun getStudentInfoByINE(INE: String): Student? {
        val parser = resources.getXml(R.xml.students_map)
        var eventType = parser.eventType
        var currentStudent: Student? = null
        var currentNumSerie: String? = null
        var currentNom: String? = null
        var currentPrenom: String? = null
        var currentNumEtudiant: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "numSerie" -> currentNumSerie = parser.nextText()
                        "nom" -> currentNom = parser.nextText()
                        "prenom" -> currentPrenom = parser.nextText()
                        "numEtudiant" -> currentNumEtudiant = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "etudiant") {
                        if (currentNumEtudiant?.lowercase(Locale.ROOT) == INE.lowercase(Locale.ROOT)) {
                            currentStudent = Student(currentNumSerie?: "", currentNom ?: "", currentPrenom ?: "", currentNumEtudiant )
                            break
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        Log.i("NFC", currentStudent.toString())

        return currentStudent
    }
    data class Student(
        val cardSerialNumber: String,
        val nom: String,
        val prenom: String,
        val numEtudiant: String
    )

    fun deleteSelectedRows(view: View) {
        tableDataList.removeIf { tableData -> tableData.isChecked }
        adapter.notifyDataSetChanged()
    }
    fun addSelectedRows(view: View) {

        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.menu_layout, null)
        dialogBuilder.setView(dialogView)

        val nom = dialogView.findViewById<EditText>(R.id.Nom)
        val prenom = dialogView.findViewById<EditText>(R.id.Prenom)
        val INE = dialogView.findViewById<EditText>(R.id.INE)

        val btnValidate = dialogView.findViewById<Button>(R.id.btnValidate)

        val alertDialog = dialogBuilder.create()

        btnValidate.setOnClickListener {
            val inputNom = nom.text.toString()
            val inputPrenom = prenom.text.toString()
            val inputINE = INE.text.toString()

            // Code pour valider les données et ajouter la ligne
            // Par exemple, ajouter la ligne à une liste ou à une base de données
            val student = getStudentInfoByINE(inputINE)
            if (student != null) {
                addTableRow(student.nom, student.prenom,inputINE,student.cardSerialNumber)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    fun validerSelectedRows(view: View) {
        Toast.makeText(this, "Validé", Toast.LENGTH_LONG).show()

        val builder = StringBuilder()
        val profName = "ProfName" // à remplacer par le nom du professeur

        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")

        builder.append("<prof> ${profName}</prof>")

        builder.append("<etudiants>\n")

        for (student in tableDataList) {
            builder.append("  <etudiant>\n")
            builder.append("    <nom>${student.Nom}</nom>\n")
            builder.append("    <prenom>${student.Prenom}</prenom>\n")
            builder.append("    <numEtudiant>${student.INE}</numEtudiant>\n")
            builder.append("  </etudiant>\n")
        }

        builder.append("</etudiants>")

        // Créer le nom du fichier
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val className = "CLASSESNAME"
        val fileName = "${timestamp}_${className}.xml"

        // Écrire les données dans un fichier
        val file = File(getExternalFilesDir("TodayCalls"), fileName)
        FileOutputStream(file).use {
            it.write(builder.toString().toByteArray())
        }
    }
}