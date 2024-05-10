package com.app.appelnfc

data class TableData(val Nom: String, val Prenom: String,val INE: String,val SerialNbr: String, var isChecked: Boolean = false) {
    override fun toString(): String {
        return "Nom: $Nom\n" +
                "Prenom: $Prenom\n" +
                "INE: $INE\n" +
                "SerialNbr: $SerialNbr\n"
    }
}
