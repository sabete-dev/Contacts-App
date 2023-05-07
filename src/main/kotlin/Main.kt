import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.time.format.DateTimeFormatter
import java.io.File
import java.lang.reflect.Type

fun main() {
    val moshi = Moshi.Builder()
        .add(ContactJsonAdapterFactory())
        .add(KotlinJsonAdapterFactory())
        .build()

    val recordsType = Types.newParameterizedType(List::class.java, Contact::class.java)
    val recordsAdapter: JsonAdapter<List<Contact>> = moshi.adapter(recordsType)

    val file = File("contacts.json")

    val phoneBook = if (file.length() == 0L) mutableListOf()
    else recordsAdapter.fromJson(file.readText())!!.toMutableList()

    try {
        menu(phoneBook, file, recordsAdapter)
    } catch (e: Exception) {
        return
    }

}

fun menu(records: MutableList<Contact>, file: File, recordsAdapter: JsonAdapter<List<Contact>>) {
    while (true) {
        print("[menu] Enter action (add, list, search, count, exit): ")
        when (readln()) {
            "add" -> addRecord(records)
            "list" -> listMenu(records, file, recordsAdapter)
            "search" -> searchMenu(records, file, recordsAdapter)
            "count" -> printCount(records)
            "exit" -> {
                file.writeText(recordsAdapter.toJson(records))
                throw Exception("Terminating main function")
            }

            else -> {
                println("Invalid Command!")
                continue
            }
        }
        println()
    }
}

fun searchMenu(records: MutableList<Contact>, file: File, recordsAdapter: JsonAdapter<List<Contact>>) {
    if (records.isEmpty()) {
        println("The Phone Book is Empty.")
        return
    }

    print("Enter search query: ")
    val query = readln()
    val searchResults = mutableMapOf<Contact, String>()
    val resultsMap = mutableMapOf<Int, Contact>()
    for (record in records) {
        if (record is Person) {
            when {
                record.name.contains(query, true)
                        || record.surname.contains(query, true)
                -> searchResults[record] = "${record.name} ${record.surname}"

                record.number.contains(query, true)
                -> searchResults[record] = record.number
            }

        } else {
            val organization = record as Organization
            when {
                organization.name.contains(query, true)
                -> searchResults[organization] = organization.name

                organization.address.contains(query, true)
                -> searchResults[organization] = organization.address

                organization.number.contains(query, true)
                -> searchResults[organization] = organization.number
            }
        }
    }
    val size = searchResults.size
    if (size == 0) println("No record with \"$query\" can be found!")
    else {
        println("Found $size results:")
        var count = 1
        for (result in searchResults) {
            println("$count. ${result.value}")
            resultsMap[count] = result.key
            count++
        }
        println()
    }
    while (true) {
        print("[search] Enter action ([number], back, again): ")
        val input = readln()
        when {
            Regex("\\d+").matches(input) && input.toInt() in 1..size -> {
                val element = resultsMap[input.toInt()]
                val index = records.indexOf(element)
                records[index].displayContactInfo()
                println()
                recordMenu(index, records, file, recordsAdapter)
            }

            input == "back" -> {
                println()
                menu(records, file, recordsAdapter)
            }

            input == "again" -> searchMenu(records, file, recordsAdapter)
            else -> {
                println("Invalid action!")
                continue
            }
        }
        println()
    }
}

fun recordMenu(nbr: Int, records: MutableList<Contact>, file: File, recordsAdapter: JsonAdapter<List<Contact>>) {
    while (true) {
        print("[record] Enter action (edit, delete, menu): ")
        when (readln()) {
            "edit" -> editRecord(nbr, records)
            "delete" -> deleteRecord(nbr, records)
            "menu" -> {
                println()
                menu(records, file, recordsAdapter)
            }

            else -> {
                println("Invalid action!")
                continue
            }
        }
        println()
    }
}

fun deleteRecord(nbr: Int, records: MutableList<Contact>) {
    records -= records[nbr]
    println("The record is deleted!")
}

fun editRecord(nbr: Int, records: MutableList<Contact>) {
    if (records[nbr] is Person) editPerson(nbr, records) else editOrganization(nbr, records)
}

fun editPerson(nbr: Int, records: MutableList<Contact>) {
    var field: String
    val record = records[nbr] as Person
    do {
        print("Select a field (name, surname, birth, gender, number): ")
        field = readln()
        if (field !in record.listOfProperties()) println("Please choose a word from the list!")
    } while (field !in record.listOfProperties())
    print("Enter $field: ")
    val newValue = readln()
    record.setProperty(field, newValue)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    record.timeEdit = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)
    println("The record updated!")
}

fun editOrganization(nbr: Int, records: MutableList<Contact>) {
    var field: String
    val record = records[nbr] as Organization
    do {
        print("Select a field (name, address, number): ")
        field = readln()
        if (field !in record.listOfProperties()) println("Please choose a word from the list!")
    } while (field !in record.listOfProperties())
    print("Enter $field: ")
    val newValue = readln()
    record.setProperty(field, newValue)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    record.timeEdit = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)
    println("The record updated!")
}

fun printCount(records: MutableList<Contact>) {
    println("The Phone Book has ${records.size} records.")
}

fun listMenu(records: MutableList<Contact>, file: File, recordsAdapter: JsonAdapter<List<Contact>>) {
    if (records.isEmpty()) {
        println("The Phone Book is Empty.")
        return
    }

    for (record in records) {
        println("${records.indexOf(record) + 1}. $record")
    }
    println()
    while (true) {
        print("[list] Enter action ([number], back): ")
        val input = readln()
        val index = if (Regex("\\d+").matches(input)) input.toInt() - 1 else -1
        when {
            index in records.indices -> {
                records[index].displayContactInfo()
                println()
                recordMenu(index, records, file, recordsAdapter)
            }

            input == "back" -> {
                println()
                menu(records, file, recordsAdapter)
            }

            else -> {
                println("Invalid action!")
                continue
            }
        }
        println()
    }
}

fun addRecord(records: MutableList<Contact>) {
    do {
        print("Enter the type (person, organization): ")
        val type = readln()
        if (type !in listOf("person", "organization")) println("Please choose a word from the list!")
        when (type) {
            "person" -> addPersonRecord(records)
            "organization" -> addOrganizationRecord(records)
        }
    } while (type !in listOf("person", "organization"))
}

fun addOrganizationRecord(records: MutableList<Contact>) {
    print("Enter the organization name: ")
    val name = readln()
    print("Enter the address: ")
    val address = readln()
    print("Enter the number: ")
    val number = checkNumber(readln())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val timeCreated = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)
    val timeEdit = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)

    val record = Organization(name, number, timeCreated, timeEdit, address)
    records.add(0, record)
    println("The record added.")
}

fun addPersonRecord(records: MutableList<Contact>) {
    print("Enter the name of the person: ")
    val name = readln()
    print("Enter the surname of the person: ")
    val surname = readln()
    print("Enter the birth date: ")
    val birthDate = checkBirthDate(readln())
    print("Enter the gender (M, F): ")
    val sex = checkSex(readln())
    print("Enter the number: ")
    val number = checkNumber(readln())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val timeCreated = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)
    val timeEdit = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toJavaLocalDateTime().format(formatter)

    val record = Person(name, number, timeCreated, timeEdit, surname, birthDate, sex)
    records.add(0, record)
    println("The record added.")
}

fun checkSex(sex: String): String {
    return if (sex in listOf("M", "F")) {
        sex
    } else {
        "[no data]".also { println("Wrong gender!") }
    }
}

fun checkBirthDate(birthDate: String): String {
    return if ("(([0-2][0-9])|(3[0-1]))[-/]((0[0-9])|(1[0-2]))[-/]\\d{4}"
            .toRegex().matches(birthDate)
    ) {
        birthDate
    } else {
        "[no data]".also { println("Bad birth date!") }
    }
}

fun checkNumber(number: String): String {
    return if (isValidNumber(number)) {
        number
    } else {
        "[no number]".also { println("Wrong number format!") }
    }
}

fun isValidNumber(str: String): Boolean {
    return "\\+?([0-9a-zA-Z]+([ -]\\([0-9a-zA-Z]{2,}\\))?|\\([0-9a-zA-Z]+\\))([ -][0-9a-zA-Z]{2,})*"
        .toRegex()
        .matches(str)
}

class Person(
    name: String,
    number: String,
    timeCreated: String,
    timeEdit: String,
    var surname: String,
    var birth: String,
    var gender: String
) : Contact(name, number, timeCreated, timeEdit) {

    override fun displayContactInfo() {
        println("Name: $name")
        println("Surname: $surname")
        println("Birth date: $birth")
        println("Gender: $gender")
        super.displayContactInfo()
    }

    override fun toString(): String {
        return "$name $surname"
    }

    override fun listOfProperties(): List<String> {
        return super.listOfProperties() + listOf("surname", "birth", "gender")
    }

    override fun setProperty(property: String, value: Any) {
        when (property) {
            "surname" -> surname = value as String
            "birth" -> birth = value as String
            "gender" -> gender = value as String
            else -> super.setProperty(property, value)
        }
    }

    override fun getProperty(property: String): Any? {
        return when (property) {
            "surname" -> surname
            "birth" -> birth
            "gender" -> gender
            else -> super.getProperty(property)
        }
    }
}

class Organization(
    name: String, number: String, timeCreated: String, timeEdit: String, var address: String
) : Contact(name, number, timeCreated, timeEdit) {

    override fun toString(): String {
        return name
    }

    override fun displayContactInfo() {
        println("Organization name: $name")
        println("Address: $address")
        super.displayContactInfo()
    }

    override fun listOfProperties(): List<String> {
        return super.listOfProperties() + listOf("address")
    }

    override fun setProperty(property: String, value: Any) {
        when (property) {
            "address" -> address = value as String
            else -> super.setProperty(property, value)
        }
    }

    override fun getProperty(property: String): Any? {
        return when (property) {
            "address" -> address
            else -> super.getProperty(property)
        }
    }
}

open class Contact(var name: String, var number: String, var timeCreated: String, var timeEdit: String) {

    open fun displayContactInfo() {
        println("Number: $number")
        println("Time created: $timeCreated")
        println("Time last edit: $timeEdit")
    }

    open fun listOfProperties(): List<String> {
        return listOf("name", "number")
    }

    open fun setProperty(property: String, value: Any) {
        when (property) {
            "name" -> name = value as String
            "number" -> number = value as String
        }
    }

    open fun getProperty(property: String): Any? {
        return when (property) {
            "name" -> name
            "number" -> number
            else -> null
        }
    }
}

class ContactJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type == Contact::class.java) {
            return ContactJsonAdapter(moshi)
        }
        return null
    }
}

class ContactJsonAdapter(moshi: Moshi) : JsonAdapter<Contact>() {
    private val options: JsonReader.Options = JsonReader.Options.of("address", "surname")
    private val organizationAdapter: JsonAdapter<Organization> = moshi.adapter(Organization::class.java)
    private val personAdapter: JsonAdapter<Person> = moshi.adapter(Person::class.java)

    override fun fromJson(reader: JsonReader): Contact? {
        var type = 0
        reader.peekJson().use { peeked ->
            peeked.beginObject()
            while (peeked.hasNext()) {
                when (peeked.selectName(options)) {
                    0 -> {
                        type = 1
                        peeked.skipValue()
                    }

                    1 -> {
                        type = 2
                        peeked.skipValue()
                    }

                    else -> {
                        peeked.skipName()
                        peeked.skipValue()
                    }
                }
            }
            peeked.endObject()
        }
        return when (type) {
            1 -> organizationAdapter.fromJson(reader)
            2 -> personAdapter.fromJson(reader)
            else -> null
        }
    }

    override fun toJson(writer: JsonWriter, value: Contact?) {
        when (value) {
            is Organization -> organizationAdapter.toJson(writer, value)
            is Person -> personAdapter.toJson(writer, value)
            else -> throw IllegalArgumentException("Unknown type: $value")
        }
    }
}
