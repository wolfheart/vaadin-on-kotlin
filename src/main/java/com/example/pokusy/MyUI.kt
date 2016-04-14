package com.example.pokusy

import com.example.pokusy.kotlinee.*
import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Title
import com.vaadin.data.fieldgroup.BeanFieldGroup
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Vaadin UI which demoes all the features. If not familiar with Vaadin, please check out the Vaadin tutorial.
 * @author mvy
 */
@Theme("valo")
@Title("Pokusy")
@Push(transport = Transport.WEBSOCKET_XHR)
class MyUI : UI() {

    private val log = LoggerFactory.getLogger(javaClass)

    private var personName: TextField? = null
    private var personGrid: Grid? = null
    private var createButton: Button? = null
    private var timerLabel: Label? = null
    private val timer = AtomicInteger()
    private var timerHandle: ScheduledFuture<*>? = null

    override fun init(request: VaadinRequest?) {
        log.error("INIT()")

        // the Vaadin DSL demo - build your UI, builder-style!
        verticalLayout {
            personName = textField("New Person Name")
            createButton = button("Create") {
                addClickListener { createPerson() }
            }
            // the JPA list demo - shows all instances of a particular JPA entity, allow sorting. @todo filtering
            personGrid = grid(dataSource = createContainer(Person::class.java)) {
                setColumns("id", "name")
            }
            timerLabel = label()
        }

        // the validation demo. infer validations from JSR303 annotations attached to the Person class
        val fg = BeanFieldGroup(Person::class.java)
        // use a dummy person for now. we only want to set up the validation on the personName field.
        fg.setItemDataSource(Person())
        fg.bind(personName, "name")

        // async and Push demo - show a label and periodically update its value from the server.
        timerHandle = scheduleAtFixedRate(0, 1 * SECONDS) {
            timer.incrementAndGet()
            transaction {
                // do something with the DB
            }
            access {
                timerLabel!!.value = "Timer: $timer; last added = ${lastAddedPersonCache.lastAdded}"
            }
        }
    }

    private fun createPerson() {
        personName!!.validate()
        transaction {
            val person = Person(name = personName!!.value.trim())
            em.persist(person)
            Notification.show("Persisted " + person)
            lastAddedPersonCache.lastAdded = person
        }
        personGrid!!.refresh()
        createButton!!.componentError = null
    }

    override fun detach() {
        log.error("DETACHED")
        timerHandle?.cancel(false)
        super.detach()
    }
}

/**
 * Refreshes the entire grid from the database.
 */
fun Grid.refresh() = (containerDataSource as JPAContainer<*>).refresh()
