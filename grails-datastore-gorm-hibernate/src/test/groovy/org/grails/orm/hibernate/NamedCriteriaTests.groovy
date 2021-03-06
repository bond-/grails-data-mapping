package org.grails.orm.hibernate

import grails.orm.PagedResultList

import org.hibernate.NonUniqueResultException
import org.junit.Ignore

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Jeff Brown
 */
class NamedCriteriaTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [NamedCriteriaPublication]
    }

    @Test
    @Ignore // feature deprecated
    void testDynamicFinderAppendedToNamedQueryWhichCallsAnotherNamedQuery() {
        // GRAILS-7253

        def now = new Date()

        assert new NamedCriteriaPublication(title: "Ten Day Old Paperback",
                                            datePublished: now - 10,
                                            paperback: true).save()
        assert new NamedCriteriaPublication(title: "Four Hundred Day Old Paperback",
                                            datePublished: now - 400,
                                            paperback: true).save()
        assert new NamedCriteriaPublication(title: "Ten Day Old Hardback",
                                            datePublished: now - 10,
                                            paperback: false).save()
        assert new NamedCriteriaPublication(title: "Four Hundred Day Old Hardback",
                                            datePublished: now - 400,
                                            paperback: false).save()

        def results = NamedCriteriaPublication.paperbackOrRecent().findAllByTitleLike('%Day Old%')
        assertEquals 3, results?.size()

        results = NamedCriteriaPublication.paperbackOrRecent().findAllByTitleLike('Ten Day Old%')
        assertEquals 2, results?.size()

    }

    @Test
    void testUniqueResult() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: "Ten Day Old Paperback",
                                            datePublished: now - 10,
                                            paperback: true).save()
        assert new NamedCriteriaPublication(title: "One Hundred Day Old Paperback",
                                            datePublished: now - 100,
                                            paperback: true).save()
        session.clear()

        def result = NamedCriteriaPublication.lastPublishedBefore(now - 200).list()
        assertNull result

        result = NamedCriteriaPublication.lastPublishedBefore(now - 50).find()
        assertEquals 'One Hundred Day Old Paperback', result?.title

        shouldFail(NonUniqueResultException) {
            NamedCriteriaPublication.lastPublishedBefore(now).list()
        }
    }

    @Test
    @Ignore // feature deprecated
    void testCountMethodOnSortedNamedQuery() {
        def today = new Date()
        def nextWeek = today + 7
        def lastWeek = today - 7
        assert new NamedCriteriaPublication(title: "Today's Paperback",
                datePublished: today, paperback: true).save()
        assert new NamedCriteriaPublication(title: "Today's Hardback",
                datePublished: today, paperback: false).save()

        assert new NamedCriteriaPublication(title: "Next Week's Paperback",
                datePublished: nextWeek, paperback: true).save()
        assert new NamedCriteriaPublication(title: "Next Week's Hardback",
                datePublished: nextWeek, paperback: false).save()

        assert new NamedCriteriaPublication(title: "Last Week's Paperback",
                datePublished: lastWeek, paperback: true).save()
        assert new NamedCriteriaPublication(title: "Last Week's Hardback",
                datePublished: lastWeek, paperback: false).save()

        def results = NamedCriteriaPublication.paperbacksOrderedByDatePublished.list()
        assertEquals 3, results.size()
        assertEquals "Last Week's Paperback", results[0].title
        assertEquals "Today's Paperback", results[1].title
        assertEquals "Next Week's Paperback", results[2].title
        results = NamedCriteriaPublication.paperbacksOrderedByDatePublished.list(max: 25)
        assertEquals 3, results.totalCount
        assertEquals 3, NamedCriteriaPublication.paperbacksOrderedByDatePublished.count()

        results = NamedCriteriaPublication.paperbacksOrderedByDatePublishedDescending.list()
        assertEquals 3, results.size()
        assertEquals "Last Week's Paperback", results[2].title
        assertEquals "Today's Paperback", results[1].title
        assertEquals "Next Week's Paperback", results[0].title
        results = NamedCriteriaPublication.paperbacksOrderedByDatePublishedDescending.list(max: 25)
        assertEquals 3, results.totalCount
        assertEquals 3, NamedCriteriaPublication.paperbacksOrderedByDatePublishedDescending.count()
    }

    @Test
    void testSorting() {
        def now = new Date()
        assert new NamedCriteriaPublication(title: "ZZZ New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "AAA New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "CCC New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "BBB New Paperback",
                datePublished: now - 10, paperback: true).save()

        assert new NamedCriteriaPublication(title: "ZZZ Old Paperback",
                datePublished: now - 900, paperback: true).save()
        assert new NamedCriteriaPublication(title: "AAA Old Paperback",
                datePublished: now - 900, paperback: true).save()

        session.clear()

        // verify the sort works...
        def results = NamedCriteriaPublication.paperbackAndRecent.list(sort: 'title')

        assertEquals 'wrong number of results', 4, results?.size()
        assertEquals 'wrong title', 'AAA New Paperback', results[0].title
        assertEquals 'wrong title', 'BBB New Paperback', results[1].title
        assertEquals 'wrong title', 'CCC New Paperback', results[2].title
        assertEquals 'wrong title', 'ZZZ New Paperback', results[3].title

        // verify the sort works along with additional criteria...
        results = NamedCriteriaPublication.paperbackAndRecent(sort: 'title') {
            ne 'title', 'CCC New Paperback'
        }

        assertEquals 'wrong number of results', 3, results?.size()
        assertEquals 'wrong title', 'AAA New Paperback', results[0].title
        assertEquals 'wrong title', 'BBB New Paperback', results[1].title
        assertEquals 'wrong title', 'ZZZ New Paperback', results[2].title

        // verify the order works...
        results = NamedCriteriaPublication.paperbackAndRecent.list(sort: 'title', order: 'desc')

        assertEquals 'wrong number of results', 4, results?.size()
        assertEquals 'wrong title', 'ZZZ New Paperback', results[0].title
        assertEquals 'wrong title', 'CCC New Paperback', results[1].title
        assertEquals 'wrong title', 'BBB New Paperback', results[2].title
        assertEquals 'wrong title', 'AAA New Paperback', results[3].title

        assert new NamedCriteriaPublication(title: "zzz New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "aaa New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "ccc New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: "bbb New Paperback",
                datePublished: now - 10, paperback: true).save()

        // verify the ignoreCase works
        results = NamedCriteriaPublication.paperbackAndRecent.list(sort: 'title', ignoreCase: false)

        assertEquals 'wrong number of results', 8, results?.size()
        assertEquals 'wrong title', 'AAA New Paperback', results[0].title
        assertEquals 'wrong title', 'BBB New Paperback', results[1].title
        assertEquals 'wrong title', 'CCC New Paperback', results[2].title
        assertEquals 'wrong title', 'ZZZ New Paperback', results[3].title
        assertEquals 'wrong title', 'aaa New Paperback', results[4].title
        assertEquals 'wrong title', 'bbb New Paperback', results[5].title
        assertEquals 'wrong title', 'ccc New Paperback', results[6].title
        assertEquals 'wrong title', 'zzz New Paperback', results[7].title
        results = NamedCriteriaPublication.paperbackAndRecent.list(sort: 'title', ignoreCase: true)

        assertEquals 'wrong number of results', 8, results?.size()

        // AAA and aaa should be the first 2, BBB and bbb should be the second 2 etc...
        // but we don't know if AAA or aaa comes first
        assertTrue 'AAA New Paperback was not where it should have been in the results', results.title[0..1].contains('AAA New Paperback')
        assertTrue 'aaa New Paperback was not where it should have been in the results', results.title[0..1].contains('aaa New Paperback')
        assertTrue 'BBB New Paperback was not where it should have been in the results', results.title[2..3].contains('BBB New Paperback')
        assertTrue 'bbb New Paperback was not where it should have been in the results', results.title[2..3].contains('bbb New Paperback')
        assertTrue 'CCC New Paperback was not where it should have been in the results', results.title[4..5].contains('CCC New Paperback')
        assertTrue 'ccc New Paperback was not where it should have been in the results', results.title[4..5].contains('ccc New Paperback')
        assertTrue 'ZZZ New Paperback was not where it should have been in the results', results.title[6..7].contains('ZZZ New Paperback')
        assertTrue 'zzz New Paperback was not where it should have been in the results', results.title[6..7].contains('zzz New Paperback')
    }

    @Test
    void testFindAllWhereAttachedToChainedNamedQueries() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 10, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 1000, paperback: true).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 10, paperback: true).save()

        assert new NamedCriteriaPublication(title: "Some Title",
                datePublished: now - 10, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Title",
                datePublished: now - 1000, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Title",
                datePublished: now - 10, paperback: true).save()
        session.clear()

        def results = NamedCriteriaPublication.recentPublications().publicationsWithBookInTitle().findAllWhere(paperback: true)

        assertEquals 1, results?.size()
    }

    @Test
    void testNamedQueryPassingMultipleParamsToNestedNamedQuery() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: "Some Book",
        datePublished: now - 10, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                                            datePublished: now - 1000, paperback: true).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                                            datePublished: now - 2, paperback: true).save()

        assert new NamedCriteriaPublication(title: "Some Title",
                                            datePublished: now - 2, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Title",
                                            datePublished: now - 1000, paperback: false).save()
        assert new NamedCriteriaPublication(title: "Some Title",
                                            datePublished: now - 2, paperback: true).save()
        session.clear()

        def results = NamedCriteriaPublication.thisWeeksPaperbacks().list()

        assertEquals 2, results?.size()

        results = NamedCriteriaPublication.queryThatNestsMultipleLevels().list()

        assertEquals 2, results?.size()
    }

    @Test
    void testGetAttachedToChainedNamedQueries() {
        def now = new Date()

        def oldPaperBackWithBookInTitleId =  new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 1000, paperback: true).save().id
        def newPaperBackWithBookInTitleId =  new NamedCriteriaPublication(title: "Some Book",
                datePublished: now, paperback: true).save().id

        assertNull NamedCriteriaPublication.publicationsWithBookInTitle().publishedAfter(now - 5).get(oldPaperBackWithBookInTitleId)
        assertNull NamedCriteriaPublication.publishedAfter(now - 5).publicationsWithBookInTitle().get(oldPaperBackWithBookInTitleId)
        assertNotNull NamedCriteriaPublication.publicationsWithBookInTitle().publishedAfter(now - 5).get(newPaperBackWithBookInTitleId)
        assertNotNull NamedCriteriaPublication.publishedAfter(now - 5).publicationsWithBookInTitle().get(newPaperBackWithBookInTitleId)
    }

    @Test
    void testPaginatedQueryReturnsPagedResultList() {
        6.times { cnt ->
            assert new NamedCriteriaPublication(title: "Some Paperback #${cnt}",
                      datePublished: new Date(), paperback: true).save(failOnError: true).id
        }
        def results = NamedCriteriaPublication.aPaperback.list([max: 2, offset: 1])

        assertEquals 2, results?.size()
        assertTrue "results should have been a PagedResultList but was a ${results.getClass()}", results instanceof PagedResultList
        assertEquals 6, results.totalCount
    }

    @Test
    void testPassingParamsAndAdditionalCriteria() {
        def now = new Date()

        6.times { cnt ->
            assert new NamedCriteriaPublication(title: "Some Old Book #${cnt}",
                      datePublished: now - 1000, paperback: true).save(failOnError: true).id
            assert new NamedCriteriaPublication(title: "Some New Book #${cnt}",
                      datePublished: now, paperback: true).save(failOnError: true).id
        }

        def results = NamedCriteriaPublication.publishedAfter(now - 5) {
            eq 'paperback', true
        }
        assertEquals 6, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', true
        }
        assertEquals 2, results?.size()

        results = NamedCriteriaPublication.publishedBetween(now - 5, now + 1) {
            eq 'paperback', true
        }
        assertEquals 6, results?.size()

        results = NamedCriteriaPublication.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', true
        }
        assertEquals 2, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 1005) {
            eq 'paperback', true
        }
        assertEquals 12, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 5) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = NamedCriteriaPublication.publishedBetween(now - 5, now + 1) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = NamedCriteriaPublication.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 1005) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()
    }

    @Test
    void testPropertyCapitalization() {
        def now = new Date()
        [true, false].each { isPaperback ->
            3.times {
                assert new NamedCriteriaPublication(title: "Some Book",
                        datePublished: now - 10, paperback: isPaperback).save()
            }
        }
        session.clear()

        def results = NamedCriteriaPublication.aPaperback.list()

        assertEquals 3, results.size()
    }
    /*
    void testPassingNullArgumentToAChainedNamedQuery() {
        if (notYetImplemented()) return
        // See http://jira.grails.org/browse/GRAILS-8672 and http://jira.codehaus.org/browse/GROOVY-5262

        def now = new Date()
        assert new NamedCriteriaPublication(title: "Some Book", datePublished: now).save()
        assert new NamedCriteriaPublication(title: "Some Book", datePublished: now - 10).save()
        assert new NamedCriteriaPublication(title: "Some Book", datePublished: now - 100).save()
        assert new NamedCriteriaPublication(title: "Some Book", datePublished: now - 1000).save()

        session.clear()

        // See http://jira.grails.org/browse/GRAILS-8672 and http://jira.codehaus.org/browse/GROOVY-5262
        def results = NamedCriteriaPublication.publishedAfter(null).publishedAfter(null).list()
        assertEquals 4, results?.size()

        results = NamedCriteriaPublication.publishedAfter(now - 50).publishedAfter(null).list()
        assertEquals 2, results?.size()

        results = NamedCriteriaPublication.publishedAfter(null).publishedAfter(now - 50).list()
        assertEquals 2, results?.size()
    }
*/
    @Test
    void testChainingNamedQueries() {
        def now = new Date()
        [true, false].each { isPaperback ->
            4.times {
                assert new NamedCriteriaPublication(title: "Some Book",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert new NamedCriteriaPublication(title: "Some Other Book",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert new NamedCriteriaPublication(title: "Some Other Title",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert new NamedCriteriaPublication(title: "Some Book",
                        datePublished: now - 1000, paperback: isPaperback).save()
                assert new NamedCriteriaPublication(title: "Some Other Book",
                        datePublished: now - 1000, paperback: isPaperback).save()
                assert new NamedCriteriaPublication(title: "Some Other Title",
                        datePublished: now - 1000, paperback: isPaperback).save()
            }
        }
        session.clear()

        def results = NamedCriteriaPublication.recentPublications().publicationsWithBookInTitle().list()
        assertEquals 'wrong number of books were returned from chained queries', 16, results?.size()
        results = NamedCriteriaPublication.recentPublications().publicationsWithBookInTitle().count()
        assertEquals 16, results

        results = NamedCriteriaPublication.recentPublications.publicationsWithBookInTitle.list()
        assertEquals 'wrong number of books were returned from chained queries', 16, results?.size()
        results = NamedCriteriaPublication.recentPublications.publicationsWithBookInTitle.count()
        assertEquals 16, results

        results = NamedCriteriaPublication.paperbacks().recentPublications().publicationsWithBookInTitle().list()
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()
        results = NamedCriteriaPublication.paperbacks().recentPublications().publicationsWithBookInTitle().count()
        assertEquals 8, results

        results = NamedCriteriaPublication.recentPublications().publicationsWithBookInTitle().findAllByPaperback(true)
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()

        results = NamedCriteriaPublication.paperbacks.recentPublications.publicationsWithBookInTitle.list()
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()
        results = NamedCriteriaPublication.paperbacks.recentPublications.publicationsWithBookInTitle.count()
        assertEquals 8, results
    }

    @Test
    void testChainingQueriesWithParams() {
        def now = new Date()
        def lastWeek = now - 7
        def longAgo = now - 1000
        2.times {
            assert new NamedCriteriaPublication(title: 'Some Book',
                    datePublished: now).save()
            assert new NamedCriteriaPublication(title: 'Some Title',
                    datePublished: now).save()
        }
        3.times {
            assert new NamedCriteriaPublication(title: 'Some Book',
                    datePublished: lastWeek).save()
            assert new NamedCriteriaPublication(title: 'Some Title',
                    datePublished: lastWeek).save()
        }
        4.times {
            assert new NamedCriteriaPublication(title: 'Some Book',
                    datePublished: longAgo).save()
            assert new NamedCriteriaPublication(title: 'Some Title',
                    datePublished: longAgo).save()
        }
        session.clear()

        def results = NamedCriteriaPublication.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 2, results?.size()

        results = NamedCriteriaPublication.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).count()
        assertEquals 2, results

        results = NamedCriteriaPublication.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 5, results?.size()

        results = NamedCriteriaPublication.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).count()
        assertEquals 5, results
    }

    @Test
    void testReferencingNamedQueryBeforeAnyDynamicMethodsAreInvoked() {
        // GRAILS-5809

        /*
         * currently this will work:
         *   NamedCriteriaPublication.recentPublications().list()
         * but this will not:
         *   NamedCriteriaPublication.recentPublications.list()
         *
         * the static property isn't being added to the class until
         * the first dynamic method (recentPublications(), save(), list() etc...) is
         * invoked
         */
        def publications = NamedCriteriaPublication.recentPublications.list()
        assertEquals 0, publications.size()
    }

    @Test
    void testAdditionalCriteriaClosure() {
        def now = new Date()
        6.times {
            assert new NamedCriteriaPublication(title: "Some Book",
            datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Other Book",
            datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Book",
            datePublished: now - 900).save()
        }
        session.clear()

        def publications = NamedCriteriaPublication.recentPublications {
            eq 'title', 'Some Book'
        }
        assertEquals 6, publications?.size()

        publications = NamedCriteriaPublication.recentPublications {
            like 'title', 'Some%'
        }
        assertEquals 12, publications?.size()

        publications = NamedCriteriaPublication.recentPublications(max: 3) {
            like 'title', 'Some%'
        }
        assertEquals 3, publications?.size()

        def cnt = NamedCriteriaPublication.recentPublications.count {
            eq 'title', 'Some Book'
        }
        assertEquals 6, cnt
    }

    @Test
    void testDisjunction() {
        def now = new Date()
        def oldDate = now - 2000

        assert new NamedCriteriaPublication(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = NamedCriteriaPublication.paperbackOrRecent.list()
        assertEquals 3, publications?.size()

        def titles = publications.title
    }

    @Test
    void testConjunction() {
        def now = new Date()
        def oldDate = now - 2000

        assert new NamedCriteriaPublication(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = NamedCriteriaPublication.paperbackAndRecent.list()
        assertEquals 1, publications?.size()
    }

    @Test
    void testList() {
        def now = new Date()
        assert new NamedCriteriaPublication(title: "Some New Book",
                datePublished: now - 10).save()
        assert new NamedCriteriaPublication(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        def publications = NamedCriteriaPublication.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title
    }

    @Test
    void testFindAllBy() {
        def now = new Date()
        3.times {
            assert new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 900).save()
        }
        session.clear()

        def publications = NamedCriteriaPublication.recentPublications.findAllByTitle('Some Book')

        assertEquals 3, publications?.size()
        assertEquals 'Some Book', publications[0].title
        assertEquals 'Some Book', publications[1].title
        assertEquals 'Some Book', publications[2].title
    }

    @Test
    void testFindAllByBoolean() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 10, paperback: true).save()

        def publications = NamedCriteriaPublication.recentPublications.findAllPaperbackByTitle('Some Book')

        assertEquals 2, publications?.size()
        assertEquals publications[0].title, 'Some Book'
        assertEquals publications[1].title, 'Some Book'
    }

    @Test
    void testFindByBoolean() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now - 10, paperback: true).save()

        def publication = NamedCriteriaPublication.recentPublications.findPaperbackByTitle('Some Book')

        assertEquals publication.title, 'Some Book'
    }

    @Test
    void testFindBy() {
        def now = new Date()
        assert new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 900).save()
        def recentBookId = new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 10).save().id
        session.clear()

        def publication = NamedCriteriaPublication.recentPublications.findByTitle('Some Book')

        assertEquals recentBookId, publication.id
    }

    @Test
    void testCountBy() {
        def now = new Date()
        3.times {
            assert new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert new NamedCriteriaPublication(title: "Some Book",
                    datePublished: now - 900).save()
        }
        session.clear()

        def numberOfNewBooksNamedSomeBook = NamedCriteriaPublication.recentPublications.countByTitle('Some Book')

        assertEquals 3, numberOfNewBooksNamedSomeBook
    }

    @Test
    void testListOrderBy() {
        def now = new Date()

        assert new NamedCriteriaPublication(title: "Book 1", datePublished: now).save()
        assert new NamedCriteriaPublication(title: "Book 5", datePublished: now).save()
        assert new NamedCriteriaPublication(title: "Book 3", datePublished: now - 900).save()
        assert new NamedCriteriaPublication(title: "Book 2", datePublished: now - 900).save()
        assert new NamedCriteriaPublication(title: "Book 4", datePublished: now).save()
        session.clear()

        def publications = NamedCriteriaPublication.recentPublications.listOrderByTitle()

        assertEquals 3, publications?.size()
        assertEquals 'Book 1', publications[0].title
        assertEquals 'Book 4', publications[1].title
        assertEquals 'Book 5', publications[2].title

    }

    @Test
    void testGetWithIdOfObjectWhichDoesNotMatchCriteria() {
        def now = new Date()
        def hasBookInTitle = new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 10).save()
        assert hasBookInTitle
        def doesNotHaveBookInTitle = new NamedCriteriaPublication(title: "Some Publication",
                datePublished: now - 900).save()
        assert doesNotHaveBookInTitle

        session.clear()

        def result = NamedCriteriaPublication.publicationsWithBookInTitle.get(doesNotHaveBookInTitle.id)
        assertNull result
    }

    @Test
    void testGetReturnsCorrectObject() {
        def now = new Date()
        def newPublication = new NamedCriteriaPublication(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = new NamedCriteriaPublication(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = NamedCriteriaPublication.recentPublications.get(newPublication.id)
        assert publication
        assertEquals 'Some New Book', publication.title
    }

    @Test
    void testThatParameterToGetIsConverted() {
        def now = new Date()
        def newPublication = new NamedCriteriaPublication(title: "Some New Book", datePublished: now - 10).save()
        assert newPublication
        def oldPublication = new NamedCriteriaPublication(title: "Some Old Book",
        datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = NamedCriteriaPublication.recentPublications.get(newPublication.id.toString())
        assert publication
        assertEquals 'Some New Book', publication.title
    }

    @Test
    void testGetWithNoArgument() {
        def now = new Date()
        def lastWeek = now - 7
        def lastYear = now - 365

        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: now).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: lastWeek).save()
        assert new NamedCriteriaPublication(title: 'Some Book', datePublished: lastYear).save()
        def result = NamedCriteriaPublication.publishedAfter(lastYear - 1).list()
        assert 3 == result?.size()
        result = NamedCriteriaPublication.publishedAfter(lastYear - 1).get()
        assert result instanceof NamedCriteriaPublication

        result = NamedCriteriaPublication.publishedAfter(lastWeek - 1).list()
        assert 2 == result?.size()
        result = NamedCriteriaPublication.publishedAfter(lastWeek - 1).get()
        assert result instanceof NamedCriteriaPublication

        result = NamedCriteriaPublication.publishedAfter(now - 1).list()
        assert 1 == result?.size()
        result = NamedCriteriaPublication.publishedAfter(now - 1).get()
        assert result instanceof NamedCriteriaPublication

        result = NamedCriteriaPublication.publishedAfter(now + 1).list()
        assert 0 == result?.size()
        result = NamedCriteriaPublication.publishedAfter(now + 1).get()
        assert result == null
    }

    @Test
    void testGetReturnsNull() {
        def now = new Date()
        def newPublication = new NamedCriteriaPublication(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = new NamedCriteriaPublication(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = NamedCriteriaPublication.recentPublications.get(42 + oldPublication.id)

        assert !publication
    }

    @Test
    void testCount() {
        def now = new Date()
        def newPublication = new NamedCriteriaPublication(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = new NamedCriteriaPublication(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()
        assertEquals 2, NamedCriteriaPublication.publicationsWithBookInTitle.count()
        assertEquals 1, NamedCriteriaPublication.recentPublications.count()
    }

    @Test
    void testCountWithParameterizedNamedQuery() {
        def now = new Date()
        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 10).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 10).save()
        assert new NamedCriteriaPublication(title: "Some Book",
                datePublished: now - 900).save()

        session.clear()
        assertEquals 2, NamedCriteriaPublication.recentPublicationsByTitle('Some Book').count()
    }

    @Test
    void testMaxParam() {
        (1..25).each {num ->
            new NamedCriteriaPublication(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

        def pubs = NamedCriteriaPublication.recentPublications.list(max: 10)
        assertEquals 10, pubs?.size()
    }

    @Test
    void testMaxResults() {
        (1..25).each {num ->
            new NamedCriteriaPublication(title: 'Book Title',
                    datePublished: new Date() + num).save()
        }

        def pubs = NamedCriteriaPublication.latestBooks.list()
        assertEquals 10, pubs?.size()
    }

    @Test
    void testMaxAndOffsetParam() {
        (1..25).each {num ->
            new NamedCriteriaPublication(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

        def pubs = NamedCriteriaPublication.recentPublications.list(max: 10, offset: 5)
        assertEquals 10, pubs?.size()

        (6..15).each {num ->
            assert pubs.find { it.title == "Book Number ${num}" }
        }

        pubs = NamedCriteriaPublication.recentPublications.list(max: '10', offset: '5')
        assertEquals 10, pubs?.size()
        (6..15).each {num ->
                assert pubs.find { it.title == "Book Number ${num}" }
        }
    }

    @Test
    void testFindAllWhereWithNamedQuery() {

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert new NamedCriteriaPublication(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pubs = NamedCriteriaPublication.recentPublications.findAllWhere(title: 'Book Number 2')
        assertEquals 3, pubs?.size()
    }

    @Test
    void testFindAllWhereWithNamedQueryAndDisjuction() {
        def now = new Date()
        def oldDate = now - 2000

        assert new NamedCriteriaPublication(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert new NamedCriteriaPublication(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert new NamedCriteriaPublication(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = NamedCriteriaPublication.paperbackOrRecent.findAllWhere(title: 'Old Paperback')
        assertEquals 1, publications?.size()
        publications = NamedCriteriaPublication.paperbackOrRecent.findAllWhere(title: 'Old Hardback')
        assertEquals 0, publications?.size()
        publications = NamedCriteriaPublication.paperbackOrRecent.findAllWhere(title: 'New Paperback')
        assertEquals 2, publications?.size()
    }

    @Test
    void testGetWithParameterizedNamedQuery() {
        def now = new Date()
        def recentPub = new NamedCriteriaPublication(title: "Some Title",
                            datePublished: now).save()
        def oldPub = new NamedCriteriaPublication(title: "Some Title",
                            datePublished: now - 900).save()

        def pub = NamedCriteriaPublication.recentPublicationsByTitle('Some Title').get(oldPub.id)
        session.clear()
        assertNull pub
        pub = NamedCriteriaPublication.recentPublicationsByTitle('Some Title').get(recentPub.id)
        assertEquals recentPub.id, pub?.id
    }

    @Test
    void testNamedQueryWithOneParameter() {
        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert new NamedCriteriaPublication(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pubs = NamedCriteriaPublication.recentPublicationsByTitle('Book Number 2').list()
        assertEquals 3, pubs?.size()
    }

    @Test
    void testNamedQueryWithMultipleParameters() {
        def now = new Date()
        (1..5).each {num ->
            assert new NamedCriteriaPublication(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

        def pubs = NamedCriteriaPublication.publishedBetween(now-2, now).list()
        assertEquals 3, pubs?.size()
    }

    @Test
    void testNamedQueryWithMultipleParametersAndDynamicFinder() {
        def now = new Date()
        (1..5).each {num ->
            assert new NamedCriteriaPublication(title: "Book Number ${num}",
                    datePublished: now + num).save()
            assert new NamedCriteriaPublication(title: "Another Book Number ${num}",
                    datePublished: now + num).save()
        }

        def pubs = NamedCriteriaPublication.publishedBetween(now, now + 2).findAllByTitleLike('Book%')
        assertEquals 2, pubs?.size()
    }

    @Test
    void testNamedQueryWithMultipleParametersAndMap() {
        def now = new Date()
        (1..10).each {num ->
            assert new NamedCriteriaPublication(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

        def pubs = NamedCriteriaPublication.publishedBetween(now-8, now-2).list(offset:2, max: 4)
        assertEquals 4, pubs?.size()
    }

    @Test
    void testFindWhereWithNamedQuery() {
        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert new NamedCriteriaPublication(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pub = NamedCriteriaPublication.recentPublications.findWhere(title: 'Book Number 2')
        assertEquals 'Book Number 2', pub.title
    }
}
