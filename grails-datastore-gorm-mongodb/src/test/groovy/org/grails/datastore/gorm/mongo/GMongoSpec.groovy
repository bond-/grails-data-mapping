package org.grails.datastore.gorm.mongo

import grails.gorm.tests.Person

import org.grails.datastore.gorm.mongo.bean.factory.MongoClientFactoryBean
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoMappingContext

import spock.lang.Specification

import com.mongodb.DB

class GMongoSpec extends Specification {
    void setup() {
        ExpandoMetaClass.enableGlobally()
    }
    void cleanup() {
        ExpandoMetaClass.disableGlobally()
    }

    def "Test configure and use gmongo"() {
        given:
            def gfb = new MongoClientFactoryBean()
            gfb.afterPropertiesSet()

        when:
            def gmongo = gfb.getObject()
            DB db = gmongo.getDB("test")
            db.dropDatabase()

        then:
            gmongo != null

        when:
            def ctx = new GenericApplicationContext()
            ctx.refresh()
            def datastore = new MongoDatastore(new MongoMappingContext("test"), gmongo, ctx)
            def session = datastore.connect()
            def entity = datastore.mappingContext.addPersistentEntity(Person)
            new MongoGormEnhancer(datastore).enhance entity

        then:
            Person.count() == 0

        when:
            def p = new Person(firstName:"Fred", lastName:"Flintstone").save(flush:true)

        then:
            p != null
            Person.withSession {
                Person.count() == 1
                Person.first().firstName == "Fred"

                Person.collection.count() == 1
                Person.collection.findOne(firstName:"Fred").lastName == "Flintstone"

                db[Person.collectionName].count() == 1
                db[Person.collectionName].findOne(firstName:"Fred").lastName == "Flintstone"

            }

        when:"we insert a list of maps"
            Person.withSession {
                Person.collection.insert( [ [firstName:"Bob"], [firstName:"Joe"]])
            }

        then:"It works"
            Person.count() == 3
    }
}
