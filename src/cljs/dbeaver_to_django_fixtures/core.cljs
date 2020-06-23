(ns dbeaver-to-django-fixtures.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [clojure.string :as str]
   [accountant.core :as accountant]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(def input-atom (reagent/atom "Paste a valid JSON!"))
(def output-atom (reagent/atom ""))

(defn s->django-app-name [s]
  (str/replace s #"_" "."))

(defn input-json->fixture-form [j]
  (let [table-name (-> j first first)
        contents (get j table-name)]
    (mapv (fn [x] {"model" (s->django-app-name table-name)
                   "pk" (x "id")
                   "fields" (dissoc x "id")}) contents)))

(defn clj->js->stringify [x]
  (.stringify js/JSON (clj->js x)))

(defn handle-input [e]
  (let [v (->> e .-target .-value)]
    (try
      (let [parsed-json (js->clj (.parse js/JSON v))]
        (reset! input-atom v)
        (reset! output-atom (input-json->fixture-form parsed-json)))
      (catch js/SyntaxError e))))

(defn input-area [rows cols div-style]
  [:div#output {:style div-style}
   [:textarea {:value @input-atom
               :on-change (fn [e]
                            (handle-input e))
               :rows rows :cols cols}]])

(defn output-area [rows cols div-style]
  [:div#input {:style div-style}
   [:textarea {:value    (clj->js->stringify @output-atom)
               :readOnly true
               :rows     rows :cols cols}]])

(defn main-component []
  (let [props (reagent/atom nil)]
    [:div.main
     [input-area 40 40 {:display "inline-block"}]
     [output-area 40 40 {:display "inline-block"}]]))

(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of dbeaver-to-django-fixtures"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))

(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of dbeaver-to-django-fixtures")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))

(defn about-page []
  (fn [] [:span.main
          [:h1 "About dbeaver-to-django-fixtures"]]))


;; -------------------------
;; Translate routes -> page components


(defn page-for [route]
  (case route
    :index #'main-component
    :about #'about-page
    :items #'items-page
    :item #'item-page))


;; -------------------------
;; Page mounting component


(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :about)} "About dbeaver-to-django-fixtures"]]]
       [page]
       [:footer
        [:p "dbeaver-to-django-fixtures was generated by the "
         [:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
