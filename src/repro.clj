(ns repro
  (:require [fluree.db.api :as fdb]))

(comment

  (def ledger "events/log")
  (def conn (fdb/connect "http://localhost:8090"))

  (when-not (empty? @(fdb/ledger-info conn ledger))
    @(fdb/delete-ledger conn ledger))

  ;; Create ledger
  @(fdb/new-ledger conn ledger)
  (fdb/wait-for-ledger-ready conn ledger)

  ;; Transact schema
  @(fdb/transact conn ledger [{:_id              :_collection
                               :_collection/name :event
                               :_collection/doc  "Athens semantic events."}
                              {:_id               :_predicate
                               :_predicate/name   :event/id
                               :_predicate/doc    "A globally unique event id."
                               :_predicate/unique true
                               :_predicate/type   :string}
                              {:_id             :_predicate
                               :_predicate/name :event/data
                               :_predicate/doc  "Event data serialized as an EDN string."
                               :_predicate/type :string}])

  ;; Add some sample events.
  @(fdb/transact conn ledger [{:_id        :event
                               :event/id   "uuid-1"
                               :event/data "[1 2 3]"}
                              {:_id        :event
                               :event/id   "uuid-2"
                               :event/data "[4 5 6]"}
                              {:_id        :event
                               :event/id   "uuid-3"
                               :event/data "[7 8 9]"}])

  ;; Delete the ledger and all data in it.
  @(fdb/delete-ledger conn ledger)

  ;; Recreate the ledger with a different schema.
  @(fdb/new-ledger conn ledger)
  (fdb/wait-for-ledger-ready conn ledger)
  @(fdb/transact conn ledger [{:_id              :_collection
                               :_collection/name :event
                               :_collection/doc  "Athens semantic events."}
                              {:_id               :_predicate
                               :_predicate/name   :event/id
                               :_predicate/doc    "A globally unique event id."
                               :_predicate/unique true
                               :_predicate/type   :string}
                              {:_id             :_predicate
                               :_predicate/name :event/dataaa ;; changed name
                               :_predicate/doc  "Event data serialized as an EDN string."
                               :_predicate/type :string}])
  ;; fluree on docker errors here, with log
  ;; f.db.ledger.transact - Unexpected consensus error proposing new block - clojure.lang.ExceptionInfo:  --------------- BLOCK REJECTED! Blocks out of order
  ;; sometimes I see a different log
  ;; f.d.l.txgroup.monitor - Error processing new block. Exiting tx monitor loop. - java.lang.NullPointerException: null\n	at org.apache.avro.io.DecoderFactory.binaryDecoder(DecoderFactory.java:238)\n

  ;; Query still shows old data from the deleted ledger there.
  @(fdb/query (fdb/db conn ledger) {:select [:*],
                                    :from "event"})

  (fdb/close conn))

