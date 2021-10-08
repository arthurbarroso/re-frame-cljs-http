(defproject re-frame-cljs-http/re-frame-cljs-http "0.1.0"
  :description ""
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "The same license as CLojure"}

  :test-paths ["test"]

  :profiles {:cljs {:source-paths ["src/re_frame_cljs_http"]
                    :jvm-opts ["-Xmx6G"]
                    :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                                   [org.clojure/clojurescript "1.10.879" :scope "provided"
                                    :exclusions [com.google.javascript/closure-compiler-unshaded
                                                 org.clojure/google-closure-library
                                                 org.clojure/google-closure-library-third-party]]
                                   [thheller/shadow-cljs "2.15.10" :scope "provided"]
                                   [re-frame "1.2.0" :scope "provided"]
                                   [org.clojure/core.async "1.3.618" :scope "provided"]
                                   [cljs-http "0.1.46"]]}})
