package flashcards
import java.io.File

fun main() {
    FlashCards.console()
}

object FlashCards {
    private val cards = mutableListOf<Card>()
    private var exitCommand = false
    private var cardsFileName = "cards.txt"
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

    private fun addCard(term: String, definition: String, mistakes: Int = 0) {
        Card(term, definition, mistakes).appendTo(cards)
    }

    private fun isTermExists(term: String) = cards.firstOrNull { it.term == term } != null

    private fun getTermByDefinition(definition: String) = cards.firstOrNull { it.definition == definition }?.term

    private fun isDefinitionExists(definition: String) = getTermByDefinition(definition) != null

    private fun checkCard(card: Card, definition: String) = cards.firstOrNull { it == card && it.definition == definition } != null

    private fun input(message: String) = print(message).run { read() }

    private fun getHardestCards(): List<Card> = cards.filter { card ->
        card.mistakes == cards.maxOf { it.mistakes } && card.mistakes != 0
    }

    private fun read(): String {
        val consoleInput = readln()
        consoleLog += "$consoleInput\n"
        return consoleInput
    }

    private fun print(message: Any) {
        println(message)
        consoleLog += "$message\n"
    }

    fun console() {
        while (!exitCommand) {
            when (input("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")) {
                Commands.ADD.command -> {
                    var inputtedTerm: String = input("The card:")
                    if(isTermExists(inputtedTerm)) {
                        print("The card \"$inputtedTerm\" already exists.")
                        continue
                    }
                    var inputtedDefinition = input("The definition of the card:")
                    if (isDefinitionExists(inputtedDefinition)) {
                        print("The definition \"$inputtedDefinition\" already exists.")
                        continue
                    }
                    addCard(inputtedTerm, inputtedDefinition)
                    print("The pair (\"$inputtedTerm\":\"$inputtedDefinition\") has been added")
                }
                Commands.REMOVE.command -> {
                    val cardToDelete = input("Which card?")
                    if (isTermExists(cardToDelete)) {
                        Card(cardToDelete,"").removeFrom(cards)
                        print("The card has been removed.")
                    } else {
                        print("Can't remove \"$cardToDelete\": there is no such card.")
                    }
                }
                Commands.ASK.command -> {
                    if (cards.isEmpty()) {
                        print("There are no cards")
                        continue
                    }
                    repeat(input("How many times to ask?").toInt()){
                        val randomCard = cards.random()
                        val answer = input("Print the definition of \"${randomCard.term}\":")
                        if (checkCard(randomCard, answer)) {
                            print("Correct!")
                        } else {
                            if (isDefinitionExists(answer)) {
                                print(
                                    "Wrong. The right answer is \"${randomCard.definition}\", but your definition" +
                                            " is correct for \"${getTermByDefinition(answer)}\"."
                                )
                            } else print("Wrong. The right answer is \"${randomCard.definition}\".")
                            randomCard.mistakes++
                        }
                    }
                }
                Commands.EXPORT.command -> {
                    cardsFileName = input("File name:")
                    var data = ""
                    var cardsNumber = 0
                    cards.forEach {
                        data += "{\"${it.term}\":\"${it.definition}\":${it.mistakes}},\n"
                        cardsNumber++
                    }
                    with(File(cardsFileName)) {
                        writeText(data)
                    }
                    print("$cardsNumber cards have been saved.")
                }
                Commands.IMPORT.command -> {
                    cardsFileName = input("File name:")
                    val data = with(File(cardsFileName)) {
                        if (exists()) readText(Charsets.UTF_8) else {
                            print("File not found.")
                            ""
                        }
                    }
                    var cardsImported = 0
                    Regex("""(?:\"|\')?(?<key>[\w\d]+)(?:\"|\')?(?:\:\s*)(?:\"|\')?(?<value>[\w\s-]*)(?:\"|\')?(?:\:\s*)(?<mistakes>\d*)""")
                        .findAll(data)
                        .forEach {
                            addCard(it.groups["key"]?.value ?: "",
                                it.groups["value"]?.value ?: "",
                                it.groups["mistakes"]?.value?.toInt() ?: 0)
                            cardsImported++
                        }
                    if (cardsImported > 0) print("$cardsImported cards have been loaded.")
                }
                Commands.EXIT.command -> {
                    exitCommand = true
                    print("Bye bye!")
                }
                Commands.LOG.command -> {
                    val fileLog = input("File name:")
                    with(File(fileLog)) {
                        writeText(consoleLog)
                    }
                    print("The log has been saved.")
                }
                Commands.HARDEST.command -> {
                    val hardestCardsList = getHardestCards()
                    if (hardestCardsList.isEmpty()) {
                        print("There are no cards with errors.")
                    } else if (hardestCardsList.size == 1) {
                        print("The hardest card is \"${hardestCardsList.first().term}\". " +
                                "You have ${hardestCardsList.first().mistakes} errors answering it")
                    } else {
                        print(
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
                Commands.RESET.command -> {
                    cards.forEach { it.mistakes = 0 }
                    print("Card statistics have been reset.")
                }
            }
        }
    }
}