(ns re-frame-cljs-http.http-fx-test
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer-macros [is deftest async use-fixtures]]
            [re-frame-cljs-http.http-fx]
            [re-frame.core :as re-frame]))

;; Spec from https://developer.github.com/v3/rate_limit/
(s/def ::limit int?)
(s/def ::remaining int?)
(s/def ::reset int?)
(s/def ::resource (s/keys :req-un [::limit ::remaining ::reset]))
(s/def ::core ::resource)
(s/def ::search ::resource)
(s/def ::resources (s/keys :req-un [::core ::search]))
(s/def ::api-result (s/keys :req-un [::resources]))

;; ---- FIXTURES ---------------------------------------------------------------
;; This fixture uses the re-frame.core/make-restore-fn to checkpoint and reset
;; to cleanup any dynamically registered handlers from our tests.
(defn fixture-re-frame
  []
  (let [restore-re-frame (atom nil)]
    {:before #(reset! restore-re-frame (re-frame.core/make-restore-fn))
     :after #(@restore-re-frame)}))

(use-fixtures :each (fixture-re-frame))

(re-frame/reg-event-db
  ::good-http-result
  (fn [db [_ done token result]]
    (is (= "test-token1" token) "expected: token passed through")
    ;; check shape of result using loose Spec
    (is (not= (s/conform ::api-result result) :cljs.spec/invalid
              (s/explain-str ::api-result result)))
    (is (every? keyword? (keys result)) "keys should be keywords")
    (done)
    db))

(re-frame/reg-event-db
  ::good-post-result
  (fn [db [_ done data result]]
    (is (= (-> result :body :data js->clj) "{\"here\":[\"is\",\"a\",\"map\"]}"))
    (done)
    db))

(re-frame/reg-event-db
  ::bad-post-result
  (fn [db [_ done data result]]
    (is (= (-> result :body :data js->clj) "{\"here\":[\"is\",\"a\",\"map\"]}"))
    (done)
    db))

(re-frame/reg-event-db
  ::good-post-body-result
  (fn [db [_ done result]]
    (let [form-data (-> result :body :form)]
      (is (= (:username form-data) "bob"))
      (is (= (:password form-data) "sekrit"))
      (done)
      db)))

;; setup failure handler
(re-frame/reg-event-db
  ::bad-http-result
  (fn [db [_ done token error]]
    (is (= "test-token1" token) "expected: token passed through")
    (cljs.test/do-report
      {:type :fail
       :message "Unexpected HTTP error, something wrong with your internet?"
       :expected ::good-http-result
       :actual error})
    (done)
    db))

;; setup request handler
(re-frame/reg-event-db
  ::on-http-request
  (fn [db [_ done id req]]
    (is (= id "my-id") "expected: id passed through")
    (done)
    db))

(re-frame/reg-event-fx
  ::http-test
  (fn [_world [_ val]]
    {:http-cljs val}))

(deftest cljs-http-get-test
  (async done
         (re-frame/dispatch [::http-test {:method :get
                                          :url "https://httpbin.org/status/200"
                                          :timeout 5000
                                          :on-request [::on-http-request done "my-id"]
                                          :on-success [::good-http-result done "test-token1"]
                                          :on-failure [::bad-http-result done "test-token1"]}])))

(deftest cljs-http-post-params-test
  (async done
         (let [data {:here ["is" "a" "map"]}]
           (re-frame/dispatch [::http-test {:method :post
                                            :url "https://httpbin.org/post"
                                            :params data
                                            :timeout 5000
                                            :format :json
                                            :on-success [::good-post-result done data]
                                            :on-failure [::bad-post-result done "test-token1"]}]))))

(deftest cljs-http-post-body-test
  (async done
         (re-frame/dispatch [::http-test {:method :post
                                          :url "https://httpbin.org/post"
                                          :params (doto (js/FormData.)
                                                    (.append "username" "bob")
                                                    (.append "password" "sekrit"))
                                          :timeout 5000
                                          :format :form
                                          :on-success [::good-post-body-result done]
                                          :on-failure [::bad-http-result done "test-token1"]}])))

(deftest cljs-http-get-seq-test
  (async done
         (re-frame/dispatch [::http-test
                             [{:method :get
                               :url "https://api.github.com/rate_limit"
                               :timeout 5000
                               :on-request [::on-http-request done "my-id"]
                               :on-success [::good-http-result done "test-token1"]
                               :on-failure [::bad-http-result done "test-token1"]}]])))

