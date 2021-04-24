(ns datomworld.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["ol" :as ol]
            ["ol/interaction" :as ol.interaction] 
            ["ol/layer/Tile"  :default ol.layer.Tile]
            ["ol/layer/Vector" :default ol.layer.VectorLayer]
            ["ol/source" :as ol.source]
            ["ol/source/Vector" :default VectorSource ]
            ["ol/proj" :as ol.proj :refer [fromLonLat]]
            ["ol/Feature" :default ol.Feature]
            ["ol/geom/Point" :default ol.geom.Point]
            ["ol/style" :refer [Icon Style]]
            ))

(enable-console-print!)

(def init-openlayer (let [full-screen? (atom false)]
                      (fn [{:keys [dom-id]}]
                        (let [rome (ol.Feature. #js{:geometry (ol.geom.Point. (fromLonLat #js[12.5, 41.9]))})
                              london (ol.Feature. #js{:geometry (ol.geom.Point. (fromLonLat #js[-0.12755, 51.507222]))})
                              red-square (Style. #js{:image (Icon. #js{:color "red"
                                                                       :crossOrigin "anonymous"
                                                                       :imgSize #js[20 20]
                                                                       :src "https://openlayers.org/en/latest/examples/data/square.svg"})})
                              _ (.setStyle rome red-square)
                              _ (.setStyle london red-square)
                              
                              vector-source (VectorSource. #js{:features #js[rome london]})
                              vector-layer (ol.layer.VectorLayer. #js{:source vector-source})

                              param (clj->js {:target dom-id
                                              :layers #js[
                                                          (ol.layer.Tile. #js{:source (ol.source/OSM.)})
                                                          vector-layer
                                                          ]
                                              :view (ol/View. #js{:center (ol.proj/fromLonLat #js[109.22367 13.77648 ])
                                                                  :zoom 0})
                                              :interactions (ol.interaction/defaults #js{:doubleClickZoom false})})
                              ol-map (ol/Map. param)]
                          (.. ol-map (on "dblclick" (fn []
                                                      (if @full-screen?
                                                        (do
                                                          (js/alert "Exit Full Screen")
                                                          (js/document.exitFullscreen))
                                                        (do
                                                          (js/alert "Enter Full Screen")
                                                          (js/document.documentElement.requestFullscreen)))
                                                      (swap! full-screen? not))))
                          ol-map))))

(defn init-materialize-ui []
  (js/M.AutoInit)
  #_(js/document.addEventListener "DOMContentLoaded"
                                (fn []
                                  (let [elements (js/document.querySelectorAll ".sidenav")]
                                    (.init js/M.Sidenav elements)))))

(defn nav-bar []
  [:div
   [:nav  
    [:div {:class "nav-wrapper"}

     [:a {:href "#", :data-target "mobile-demo", :class "sidenav-trigger show-on-large"}
      [:i {:class "material-icons"} "menu"]]
     [:ul {:class "right hide-on-med-and-down"}
      [:li 
       [:a {:href "sass.html"} "Sass"]]
      [:li 
       [:a {:href "badges.html"} "Components"]]
      [:li 
       [:a {:href "collapsible.html"} "Javascript"]]
      [:li 
       [:a {:href "mobile.html"} "Mobile"]]]]]

   [:ul {:class "sidenav", :id "mobile-demo"}
    [:li 
     [:a {:href "sass.html"} "Sass"]]
    [:li 
     [:a {:href "badges.html"} "Components"]]
    [:li 
     [:a {:href "collapsible.html"} "Javascript"]]
    [:li 
     [:a {:href "mobile.html"} "Mobile"]]]])

(def map-view (r/create-class {:component-did-mount (fn [this-component]

                                                      (init-openlayer {:dom-id "map"})
                                                      (init-materialize-ui))
                               :reagent-render (fn []
                                                 [:div {:style {:width "100%" :height "100%"}}
                                                  ;;[nav-bar]
                                                  
                                                  [:div#map {:style {:width "100%"  :height "100%" :margin-top 1 }}]])}))
[]


(defn init []
  (let [app (js/document.getElementById "app")]
    (rdom/render [map-view] app)))

