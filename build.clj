(ns build
  (:require
    [clojure.tools.build.api :as b]
    [codox.main :as codox]))

; coordinates
(def lib 'org.sbrubbles/conditio-clj)
(def version "0.1.0")

; directories
(def src-dir "src")
(def dist-dir "target")
(def class-dir (str dist-dir "/classes"))
(def doc-dir (str dist-dir "/doc"))
(def basis (b/create-basis {:project "deps.edn"}))

; files
(def ignore-files [#"user.clj"])
(def jar-file (format (str dist-dir "/%s-%s.jar") (name lib) version))

; metadata
(def description "A simple condition system for Clojure, without too much machinery.")
(def scm-url "https://github.com/hanjos/conditio-clj")

(def pom-template
  [[:description description]
   [:url scm-url]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url (str scm-url "/blob/main/LICENSE")]]]])

(defn- echo [opts & args]
  (when (:verbose opts) (println (apply str args))))

(defn clean [opts]
  (echo opts "Cleaning " dist-dir "...")
  (b/delete {:path dist-dir})

  opts)

(defn jar [opts]
  (echo opts "Writing POM...")
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  [src-dir]
                :pom-data  pom-template})

  (echo opts "Copying sources...")
  (b/copy-dir {:src-dirs   [src-dir]
               :target-dir class-dir
               :ignores    ignore-files})

  (echo opts "Creating the JAR...")
  (b/jar {:class-dir class-dir
          :jar-file  jar-file})

  opts)

(defn doc [opts]
  (echo opts "Generating docs...")
  (codox/generate-docs {:language     :clojure
                        :output-path  doc-dir
                        :source-paths [src-dir]
                        :namespaces ['org.sbrubbles.conditio]
                        :exclude-vars #"^(map)?->\p{Upper}"
                        :metadata     {}
                        :themes       [:default]}))

