(ns build
  (:require
    [clojure.tools.build.api :as b]
    [codox.main :as codox]))

; coordinates
(def metadata
  {:lib 'org.sbrubbles/conditio-clj
   :version "0.1.0"
   :pom-data [[:description "A simple condition system for Clojure, without too much machinery."]
              [:url "https://github.com/hanjos/conditio-clj"]
              [:licenses
               [:license
                [:name "MIT License"]
                [:url "https://github.com/hanjos/conditio-clj/blob/main/LICENSE"]]]]})

; directories
(def src-dir "src")
(def dist-dir "target")
(def class-dir (str dist-dir "/classes"))
(def doc-dir (str dist-dir "/doc"))
(def basis (b/create-basis {:project "deps.edn"}))

; files
(def ignore-files [#"user.clj"])
(def jar-file (format (str dist-dir "/%s-%s.jar")
                      (name (:lib metadata))
                      (:version metadata)))

; helper functions
(defn- echo [opts & [args]]
  (when (:verbose opts) (println (apply str args))))

; available commands
(defn clean [opts]
  (echo opts "Cleaning " dist-dir "...")
  (b/delete {:path dist-dir})

  opts)

(defn jar [opts]
  (echo opts "Writing POM...")
  (b/write-pom {:class-dir class-dir
                :lib       (:lib metadata)
                :version   (:version metadata)
                :basis     basis
                :src-dirs  [src-dir]
                :pom-data  (:pom-data metadata)})

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
  (codox/generate-docs {:name "conditio-clj"
                        :version (:version metadata)
                        :description (get-in metadata [:pom-data 0 1])
                        :language     :clojure
                        :output-path  doc-dir
                        :source-paths [src-dir]
                        :namespaces ['org.sbrubbles.conditio]
                        :exclude-vars #"^(map)?->\p{Upper}"
                        :metadata     {:doc/format :markdown}
                        :themes       [:default]}))

(defn version [opts]
  (println (:version metadata))

  opts)