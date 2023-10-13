(ns build
  (:require [clojure.tools.build.api :as b]))

; coordinates
(def lib 'org.sbrubbles/conditio-clj)
(def version "0.1.0")

; directories
(def src-dir "src")
(def dist-dir "target")
(def class-dir (str dist-dir "/classes"))
(def basis (b/create-basis {:project "deps.edn"}))

; files
(def ignore-files [#"user.clj"])
(def jar-file (format (str dist-dir "/%s-%s.jar") (name lib) version))

(defn- echo [opts & args]
  (when (:verbose opts) (println (apply str args))))

(defn clean [opts]
  (echo opts "Cleaning " dist-dir "...")

  (b/delete {:path dist-dir})

  opts)

(defn jar [opts]
  (echo opts "Writing POM...")
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs [src-dir]})

  (echo opts "Copying sources...")
  (b/copy-dir {:src-dirs [src-dir]
               :target-dir class-dir
               :ignores ignore-files})

  (echo opts "Creating the JAR...")
  (b/jar {:class-dir class-dir
          :jar-file jar-file})

  opts)