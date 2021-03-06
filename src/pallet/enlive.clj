(ns pallet.enlive
  "Wrappers for enlive to enable template specialisation and use xml."
  (:use
   [pallet.template :only [find-template]]
   clojure.contrib.logging)
  (:require
   [net.cgrand.enlive-html :as enlive]))

;; (def memo-nodes
;;      (memoize
;;       (fn [source]
;;         (if-let [source (find-template source)]
;;           (map enlive/annotate
;;                (enlive/select
;;                 (enlive/xml-resource source) [:body :> enlive/any-node]))))))

;; (defmacro deffragment
;;   [name source args & forms]
;;   `(def ~name
;;         (fn ~args
;;           (if-let [nodes# (memo-nodes ~source)]
;;             (enlive/flatmap
;;              (enlive/transformation ~@forms) nodes#)))))

(defn elt
 ([tag] (elt tag nil))
 ([tag attrs & content]
   {:tag tag
    :attrs attrs
    :content content}))

(defmacro transform-nodes
  [[nodes] & forms]
  `(enlive/flatmap (enlive/transformation ~@forms) ~nodes))

(defmacro deffragment
  [name args & forms]
  `(defn ~name ~args
     (fn [nodes#] (enlive/at nodes# ~@forms))))

(def memo-xml-resource
     (memoize
      (fn [source node-type]
        (if-let [source (find-template source node-type)]
          (enlive/xml-resource source)
          (error (str "No template found for " source " " (node-type :tag)))))))

(defmacro defsnippet
  "A snippet returns a collection of nodes."
  [name source node-type args & forms]
  `(defn ~name ~args
    (if-let [nodes# (memo-xml-resource ~source ~node-type)]
      (enlive/at nodes# ~@forms))))

(defmacro xml-template
  "A template returns a seq of string:
   Overridden from enlive to defer evaluation of the source until runtime, and
   to enable specialisation on node-type"
  [source node-type args & forms]
  `(comp enlive/emit*
         (fn ~args
           (if-let [nodes# (memo-xml-resource ~source ~node-type)]
             (enlive/flatmap (enlive/transformation ~@forms) nodes#)))))

(defn xml-emit
  "Emit a template, adding an XML Declaration."
  [f & args]
  (str "<?xml version='1.0' encoding='utf-8'?>\n"
       (apply str (apply f args))))

(defmacro transform-if [expr transform]
  `(if ~expr ~transform identity))

(defmacro transform-if-let [binding transform]
  `(if-let ~binding ~transform identity))
