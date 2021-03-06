(ns kibit.rules.misc
  (:require [clojure.core.logic :as logic])
  (:use [kibit.rules.util :only [defrules]]))

(defrules rules
  ;; clojure.string
  [(apply str (interpose ?x ?y)) (clojure.string/join ?x ?y)]
  [(apply str (reverse ?x)) (clojure.string/reverse ?x)]

  ;; mapcat
  [(apply concat (apply map ?x ?y)) (mapcat ?x ?y)]
  [(apply concat (map ?x . ?y)) (mapcat ?x . ?y)]

  ;; filter
  [(filter (complement ?pred) ?coll) (remove ?pred ?coll)]
  [(filter #(not (?pred ?x)) ?coll) (remove ?pred ?coll)]

  ;; Unneeded anonymous functions
  (let [fun (logic/lvar)
        args (logic/lvar)]
    [(fn [expr]
       (logic/all
        (logic/conde
         [(logic/== expr (list 'fn args (logic/llist fun args)))]
         [(logic/== expr (list 'fn* args (logic/llist fun args)))])
        (logic/pred fun #(or (keyword? %)
                             (and (symbol? %)
                                  (not= \. (first (str %)))
                                  (not-any? #{\/} (str %)))))))
     #(logic/== % fun)])
  
   ;; do
  [(do ?x) ?x]

  ;; Java stuff
  [(.toString ?x) (str ?x)]
  
  (let [obj (logic/lvar)
        method (logic/lvar)
        args (logic/lvar)]
    [#(logic/== % (logic/llist '. obj method args))
     #(logic/project [method args]
        (logic/== % `(~(symbol (str "." method)) ~obj ~@args)))])

  
  ;; Threading
  (let [form (logic/lvar)
        arg (logic/lvar)]
    [#(logic/all (logic/== % (list '-> arg form)))
     (fn [sbst]
       (logic/conde
        [(logic/all
          (logic/pred form #(or (symbol? %) (keyword? %)))
          (logic/== sbst (list form arg)))]
        [(logic/all
          (logic/pred form seq?)
          (logic/project [form]
            (logic/== sbst (list* (first form) arg (rest form)))))]))])

  (let [form (logic/lvar)
        arg (logic/lvar)]
    [#(logic/all (logic/== % (list '->> arg form)))
     (fn [sbst]
       (logic/conde
        [(logic/all
          (logic/pred form #(or (symbol? %) (keyword? %)))
          (logic/== sbst (list form arg)))]
        [(logic/all
          (logic/pred form seq?)
          (logic/project [form]
            (logic/== sbst (concat form (list arg)))))]))])

  

  ;; Other
  [(not (some ?pred ?coll)) (not-any? ?pred ?coll)]
  [(not (= . ?args)) (not= . ?args)])


(comment
  (apply concat (apply map f (apply str (interpose \, "Hello"))))
  (filter (complement nil?) [1 2 3])

  (.toString (apply str (reverse "Hello")))

  (map (fn [x] (inc x)) [1 2 3])
  (map (fn [x] (.method x)) [1 2 3])
  (map #(dec %) [1 2 3])
  (map #(.method %) [1 2 3])
  (map #(Double/parseDouble %) [1 2 3])
  (map (fn [x] (Integer/parseInteger x))
       [1 2 3])

  
  (map (fn [m] (:key m)) [some maps])
  (map (fn [m] (:key m alt)) [a b c])

  (. obj toString)
  (. obj toString a b c)

  (-> x f) ;; (f x)
  (-> x (f a b)) ;; (f x a b)
  (-> x (f)) ;; (f x)

  (->> x f) ;; (f x)
  (->> x (f a b)) ;; (f a b x)
  (->> x (f)) ;; (f x)
  
  )
