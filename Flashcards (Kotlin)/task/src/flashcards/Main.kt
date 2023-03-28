package flashcards

import java.io.File

fun main(args: Array<String>) {
    FlashCards.console(args)
}

object FlashCards {
    private val cards = mutableListOf<Card>()
    private var exitCommand = false
    private var importFileName = ""
    private var exportFileName = ""
    private var consoleLog: String = ""

    data class Card(var term: String, var definition: String, var mistakes: Int = 0) {
        fun appendTo(list: MutableList<Card>) {
            if (list.contains(this)) {
                list.first { it == this }.definition = this.definition
            } else list.add(this)
        }

        override fun equals(other: Any?): Boolean {
            return (other is Card) && (term == other.term)
        }

        fun removeFrom(list: MutableList<Card>) {
            list.remove(this)
        }
    }

    enum class Commands(val command: String) {
        ADD("add"), REMOVE("remove"), ASK("ask"),
        IMPORT("import"), EXPORT("export"), EXIT("exit"),
        LOG("log"), HARDEST("hardest card"), RESET("reset stats"),
    }

    private fun processArguments(args: Array<String>) {
        if (args.isNotEmpty()) {
            val importRegex = Regex("""(?:-import\s+)(?<import>[\w]+\.[\w]+)(?:\s*)""")
            val exportRegex = Regex("""(?:-export\s+)(?<export>[\w]+\.[\w]+)(?:\s*)""")
            val argsString = args.joinToString(" ")

            importFileName = importRegex.find(argsString)?.groups?.get("import")?.value ?: ""
            exportFileName = exportRegex.find(argsString)?.groups?.get("export")?.value ?: ""
        }
    }

    private fun addCard(term: String, definition: String, mistakes: Int = 0) {
        Card(term, definition, mistakes).appendTo(cards)
    }

    private fun isTermExists(term: String) = cards.firstOrNull { it.term == term } != null

    private fun getTermByDefinition(definition: String) = cards.firstOrNull { it.definition == definition }?.term

    private fun isDefinitionExists(definition: String) = getTermByDefinition(definition) != null

    private fun checkCard(card: Card, definition: String) =
        cards.firstOrNull { it == card && it.definition == definition } != null

    private fun input(message: String) = userPrint(message).run { userInput() }

    private fun isExportFileArgumentReceived() = exportFileName != ""
    private fun isImportFileArgumentReceived() = importFileName != ""

    private fun getHardestCards(): List<Card> = cards.filter { card ->
        card.mistakes == cards.maxOf { it.mistakes } && card.mistakes != 0
    }

    private fun userInput(): String {
        val consoleInput = readln()
        consoleLog += "$consoleInput\n"
        return consoleInput
    }

    private fun userPrint(message: Any) {
        println(message)
        consoleLog += "$message\n"
    }

    fun console(args: Array<String>) {
        processArguments(args)
        loadCardsFromFile()

        while (!exitCommand) {
            when (input("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")) {
                Commands.ADD.command -> {
                    addCardAction()
                }

                Commands.REMOVE.command -> {
                    removeCardAction()
                }

                Commands.ASK.command -> {
                    askCardsFromUserAction()
                }

                Commands.EXPORT.command -> {
                    manualExportCommandAction()
                }

                Commands.IMPORT.command -> {
                    manualImportCommandAction()
                }

                Commands.EXIT.command -> {
                    exitCommandAction()
                }

                Commands.LOG.command -> {
                    saveLogAction()
                }

                Commands.HARDEST.command -> {
                    showHardestCardsAction()
                }

                Commands.RESET.command -> {
                    resetMistakeStatisticsAction()
                }
            }
        }
    }

    private fun manualExportCommandAction() {
        val cardsNumber = exportToFile(input("File name:"))
        userPrint("$cardsNumber cards have been saved.")
    }

    private fun manualImportCommandAction() {
        val cardsImported = importFromFile(input("File name:"))
        if (cardsImported > 0) userPrint("$cardsImported cards have been loaded.")
    }

    private fun exitCommandAction() {
        exitCommand = true
        userPrint("Bye bye!")
        if (isExportFileArgumentReceived()) {
            val cardSaved = exportToFile(exportFileName)
            userPrint("$cardSaved cards have been saved.")
        }
    }

    private fun resetMistakeStatisticsAction() {
        cards.forEach { it.mistakes = 0 }
        userPrint("Card statistics have been reset.")
    }

    private fun showHardestCardsAction() {
        val hardestCardsList = getHardestCards()
        if (hardestCardsList.isEmpty()) {
            userPrint("There are no cards with errors.")
        } else if (hardestCardsList.size == 1) {
            userPrint(
                "The hardest card is \"${hardestCardsList.first().term}\". " +
                        "You have ${hardestCardsList.first().mistakes} errors answering it"
            )
        } else {
            userPrint(
                "The hardest cards are ${
                    hardestCardsList.joinToString(
                        prefix = "\"",
                        separator = "\", \"",
                        postfix = "\""
                    ) { it.term }
                }. You have ${hardestCardsList.first().mistakes} errors answering them."
            )
        }
    }

    private fun saveLogAction() {
        val fileLog = input("File name:")
        with(File(fileLog)) {
            writeText(consoleLog)
        }
        userPrint("The log has been saved.")
    }

    private fun askCardsFromUserAction() {
        if (cards.isEmpty()) {
            userPrint("There are no cards")
            return
        }
        repeat(input("How many times to ask?").toInt()) {
            val randomCard = cards.random()
            val answer = input("Print the definition of \"${randomCard.term}\":")
            if (checkCard(randomCard, answer)) {
                userPrint("Correct!")
            } else {
                if (isDefinitionExists(answer)) {
                    userPrint(
                        "Wrong. The right answer is \"${randomCard.definition}\", but your definition" +
                                " is correct for \"${getTermByDefinition(answer)}\"."
                    )
                } else userPrint("Wrong. The right answer is \"${randomCard.definition}\".")
                randomCard.mistakes++
            }
        }
    }

    private fun removeCardAction() {
        val cardToDelete = input("Which card?")
        if (isTermExists(cardToDelete)) {
            Card(cardToDelete, "").removeFrom(cards)
            userPrint("The card has been removed.")
        } else {
            userPrint("Can't remove \"$cardToDelete\": there is no such card.")
        }
    }

    private fun addCardAction() {
        var inputtedTerm: String = input("The card:")
        if (isTermExists(inputtedTerm)) {
            userPrint("The card \"$inputtedTerm\" already exists.")
            return
        }
        var inputtedDefinition = input("The definition of the card:")
        if (isDefinitionExists(inputtedDefinition)) {
            userPrint("The definition \"$inputtedDefinition\" already exists.")
            return
        }
        addCard(inputtedTerm, inputtedDefinition)
        userPrint("The pair (\"$inputtedTerm\":\"$inputtedDefinition\") has been added")
    }

    private fun loadCardsFromFile() {
        if (isImportFileArgumentReceived()) {
            userPrint("${importFromFile(importFileName)} cards have been loaded.")
        }
    }

    private fun exportToFile(cardsFileName: String): Int {
        var data = ""
        var cardsNumber = 0
        cards.forEach {
            data += "{\"${it.term}\":\"${it.definition}\":${it.mistakes}},\n"
            cardsNumber++
        }
        with(File(cardsFileName)) {
            writeText(data)
        }
        return cardsNumber
    }

    private fun importFromFile(cardsFileName: String): Int {
        val data = with(File(cardsFileName)) {
            if (exists()) readText(Charsets.UTF_8) else {
                userPrint("File not found.")
                ""
            }
        }
        var cardsImported = 0
        Regex("""(?:\"|\')?(?<key>[\w\d]+)(?:\"|\')?(?:\:\s*)(?:\"|\')?(?<value>[\w\s-]*)(?:\"|\')?(?:\:\s*)(?<mistakes>\d*)""")
            .findAll(data)
            .forEach {
                addCard(
                    it.groups["key"]?.value ?: "",
                    it.groups["value"]?.value ?: "",
                    it.groups["mistakes"]?.value?.toInt() ?: 0
                )
                cardsImported++
            }
        return cardsImported
    }
}