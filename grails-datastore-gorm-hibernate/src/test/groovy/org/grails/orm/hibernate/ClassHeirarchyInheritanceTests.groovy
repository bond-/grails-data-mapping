package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class ClassHeirarchyInheritanceTests extends AbstractGrailsHibernateTests {

    @Test
    void testPolymorphicQuery() {
        def alpha = new Alpha()
        def fiat = new Fiat()
        def ferrari = new Ferrari()

        fiat.type = "cheap"
        alpha.type = "luxury"
        ferrari.type = "luxury"

        fiat.save()
        alpha.save()
        ferrari.save()

        def cars = Car.findAll("from Car as c where c.type='luxury'")
        assertEquals 2, cars.size()

        def fiats = Fiat.list()

        assertEquals 1, fiats.size()
    }

    @Override
    protected getDomainClasses() {
        [Car, Alpha, Fiat, Ferrari]
    }
}
@Entity
class Car { Long id;Long version;String type;}
@Entity
class Alpha extends Car { }
@Entity
class Fiat extends Car { }
@Entity
class Ferrari extends Car { }

