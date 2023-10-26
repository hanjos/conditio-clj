(ns build
  (:require
    [clojure.tools.build.api :as b]
    [codox.main :as codox]))

; coordinates
(def metadata
  {:lib          'org.sbrubbles/conditio-clj
   :version      "0.2.0-SNAPSHOT"
   :dirs         {:src     "src"
                  :target  "target"
                  :classes "target/classes"
                  :doc     "target/doc"}
   :basis        (b/create-basis {:project "deps.edn"})
   :ignore-files [#"user.clj"]
   :pom-data     [[:description "A simple condition system for Clojure, without too much machinery."]
                  [:url "https://github.com/hanjos/conditio-clj"]
                  [:licenses
                   [:license
                    [:name "MIT License"]
                    [:url "https://github.com/hanjos/conditio-clj/blob/main/LICENSE"]]]
                  [:distributionManagement
                   [:repository
                    [:id "github"]
                    [:name "GitHub hanjos Apache Maven Packages"]
                    [:url "https://maven.pkg.github.com/hanjos/conditio-clj"]]]]})

; helper functions
(def jar-file
  (let [{:keys [lib version dirs]} metadata
        {:keys [target]} dirs]
    (format (str target "/%s-%s.jar")
            (name lib)
            version)))

(def flags
  ; filtering out nil values
  (into {}
        (filter second)
        {:verbose (System/getenv "CONDITIO_VERBOSE")}))

(defn echo [& args]
  (when (:verbose flags) (println (apply str args))))

; available commands
(defn clean [_]
  (let [{{:keys [target]} :dirs} metadata]
    (echo "Cleaning " target "...")
    (b/delete {:path target})))

(defn jar [_]
  (let [{:keys [lib version pom-data basis dirs ignore-files]} metadata
        {:keys [src classes]} dirs]
    (echo "Writing POM...")
    (b/write-pom {:class-dir classes
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  [src]
                  :pom-data  pom-data})

    (echo "Copying sources...")
    (b/copy-dir {:src-dirs   [src]
                 :target-dir classes
                 :ignores    ignore-files})

    (echo "Creating the JAR...")
    (b/jar {:class-dir classes
            :jar-file  jar-file})))

(defn doc [_]
  (let [{:keys [lib version dirs]} metadata
        {:keys [src doc]} dirs
        description (get-in metadata [:pom-data 0 1])]
    (echo "Generating docs...")
    (codox/generate-docs {:name         (name lib)
                          :version      version
                          :description  description
                          :language     :clojure
                          :output-path  doc
                          :source-paths [src]
                          :namespaces   ['org.sbrubbles.conditio 'org.sbrubbles.conditio.vars]
                          :exclude-vars #"^(map)?->\p{Upper}"
                          :metadata     {:doc/format :markdown}
                          :themes       [:default]})))

(defn version [_]
  (println (:version metadata)))