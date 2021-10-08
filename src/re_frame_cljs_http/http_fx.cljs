(ns re-frame-cljs-http.http-fx
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [re-frame.core :as re-frame]))

(def error-statuses
  [400 401 402 403
   404 405 406 407
   408 409 410 411
   412 413 414 415
   416 417 418 421
   422 423 424 425
   426 428 429 431
   451 500 501 502
   503 504 506 507
   508 510 511])

(defn result-handler [on-success on-failure [status response]]
  (if (contains? error-statuses status)
    (on-failure response)
    (on-success response)))

(defn http-handler [request on-success on-failure]
  (go (let [response (<! (http/request request))
            status (:status response)]
        (result-handler
          #(re-frame/dispatch (conj on-success %))
          #(re-frame/dispatch (conj on-failure %))
          [status response]))))

(defn serialize-request-params [{:as request :keys
                                   [params format]}]
  (condp = format
    :json (assoc request :json-params params)
    :edn (assoc request :edn-params params)
    :transit (assoc request :transit-params params)
    :form (assoc request :form-params params)
    (assoc request :json-params params)))

(defn request->http
  [{:as request
    :keys [on-success on-failure]
    :or {on-success [:http-no-on-success]
         on-failure [:http-no-on-failure]}}]
  (-> request
      serialize-request-params
      (http-handler on-success on-failure)))

(defn dispatch-on-request [request httpmap]
  (when-let [on-request (:on-request request)]
    (re-frame/dispatch (conj on-request httpmap))))

(defn http-effect [request]
  (let [seq-request-maps (if (sequential? request) request [request])]
    (doseq [request seq-request-maps]
      (let [httpmap (-> request request->http)]
        (dispatch-on-request request httpmap)))))

(re-frame/reg-fx :http-cljs http-effect)

(re-frame/reg-event-db
  ::good-http-result
  (fn [db [_ token result]]
    db))

(re-frame/reg-event-db
  ::good-post-result
  (fn [db [_ data result]]
    ;; httpbin returns a JSON object containing headers and other
    ;; metadata from our request along with our JSON payload.
    (println {:resultttt result})
    db))

(re-frame/reg-event-db
  ::good-post-body-result
  (fn [db [_ {:strs [form] :as result}]]
    db))

;; setup failure handler
(re-frame/reg-event-db
  ::bad-http-result
  (fn [db [_ token error]]
    (println {:token token :test-token1 "test-token1"})
    db))

;; setup request handler
(re-frame/reg-event-db
  ::on-http-request
  (fn [db [_ id req]]
    (println {:id id})
    db))

(comment
  (http-effect {:url "https://httpbin.org/post"
                :method :post
                :params {:here ["is" "a" "map"]}
                :timeout 5000
                :format :json
                :on-success [::good-http-result]
                :on-failure [::bad-http-result]})
  (http-effect
    {:method :get
     :url "https://api.github.com/rate_limit"
     :timeout 5000
     :on-request [::on-http-request "my-id"]
     :on-success [::good-http-result "test-token1"]
     :on-failure [::bad-http-result "test-token1"]})
  (http-effect
    [{:method :get
      :url "https://api.github.com/rate_limit"
      :timeout 5000
      :on-success [::good-http-result "test-token1"]
      :on-failure [::bad-http-result "test-token1"]}]))
