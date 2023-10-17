(ns build
  (:require
    [cemerick.pomegranate.aether :as aether]
    [clojure.java.io :as io]
    [clojure.tools.build.api :as b]
    [codox.main :as codox]))

; coordinates
(def metadata
  {:lib          'org.sbrubbles/conditio-clj
   :version      "0.1.0"
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

(def jar-file
  (let [{:keys [lib version dirs]} metadata
        {:keys [target]} dirs]
    (format (str target "/%s-%s.jar")
            (name lib)
            version)))

; helper functions
(def flags
  (into {} ; filtering out nil values
        (filter second)
        {:verbose (System/getenv "CONDITIO_VERBOSE")}))

(defn echo [& args]
  (when (:verbose flags) (println (apply str args))))

; available commands
(defn clean [opts]
  (let [{{:keys [target]} :dirs} metadata]
    (echo "Cleaning " target "...")
    (b/delete {:path target}))

  opts)

(defn jar [opts]
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
            :jar-file  jar-file}))

  opts)

(defn doc [opts]
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
                          :namespaces   ['org.sbrubbles.conditio]
                          :exclude-vars #"^(map)?->\p{Upper}"
                          :metadata     {:doc/format :markdown}
                          :themes       [:default]}))

  opts)

(defn deploy [opts]
  (let [{:keys [lib version dirs]} metadata
        {:keys [classes]} dirs
        repo-url (get-in metadata [:pom-data 3 1 3 1])]
    (echo "Deploying...")
    (aether/deploy :coordinates [lib version]
                   :jar-file (io/file jar-file)
                   :pom-file (io/file (str classes
                                           "/META-INF/maven/"
                                           lib
                                           "/pom.xml"))
                   :repository {:url      repo-url
                                :username (System/getenv "USERNAME")
                                :password (System/getenv "TOKEN")}))

  opts)

(defn version [opts]
  (println (:version metadata))

  opts)