(ns datomworld.core
  (:require [clojure.core.async :as a :include-macros true]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [stigmergy.mercury :as m]
            
            ["ol" :as ol]
            ["ol/interaction" :as ol.interaction] 
            ["ol/layer/Tile"  :default ol.layer.Tile]
            ["ol/layer/Vector" :default ol.layer.VectorLayer]
            ["ol/source" :as ol.source]
            ["ol/source/Vector" :default VectorSource ]
            ["ol/proj" :as ol.proj :refer [fromLonLat]]
            ["ol/Feature" :default ol.Feature]
            ["ol/geom/Point" :default ol.geom.Point]
            ["ol/style" :refer [Icon Style]]))

(enable-console-print!)

(js/navigator.geolocation.watchPosition (fn [pos]
                                          (let [lon (aget pos "coords" "longitude")
                                                lat (aget pos "coords" "latitude")]
                                            (m/broadcast [:here {:latitude lat
                                                                 :longitude lon}])))
                                        #(prn %)
                                        #js{:enableHighAccuracy true
                                            :timeout 5000
                                            :maximumAge 0})

(def get-lon-lat (let [c (a/chan)]
                   (fn []
                     (.. js/navigator.geolocation
                         (getCurrentPosition (fn [position]
                                               (let [lon (.-longitude position.coords)
                                                     lat (.-latitude position.coords)]
                                                 (a/put! c [lon lat])))))
                     c)))

;;https://gis.stackexchange.com/questions/214400/dynamically-update-position-of-geolocation-marker-in-openlayers-3
(def init-openlayer (let [full-screen? (atom false)
                          red-square (Style. #js{:image (Icon. #js{:color "red"
                                                                   :crossOrigin "anonymous"
                                                                   :imgSize #js[20 20]
                                                                   :src "https://openlayers.org/en/latest/examples/data/square.svg"
                                                                   })})
                          
                          vector-source (VectorSource.)
                          vector-layer (ol.layer.VectorLayer. #js{:source vector-source})
                          here-pt (ol.geom.Point. (fromLonLat #js[0 0]))
                          view (ol/View. (clj->js {:center (ol.proj/fromLonLat #js[0 0])
                                                   :zoom 15}))]
                      
                      (m/on :here (fn [[_ {:keys [longitude latitude]} :as msg]]
                                    (prn msg)
                                    (let [lon-lat (clj->js [longitude latitude])
                                          coords (fromLonLat lon-lat)]
                                      (.. here-pt (setCoordinates coords))
                                      (.. view (animate (clj->js {:center coords
                                                                  :duration 500}))))))
                      
                      (fn [{:keys [dom-id]}]
                        (a/go
                          (let [current-point (ol.Feature. (clj->js {:geometry here-pt}))
                                _ (.setStyle current-point red-square)
                                _ (.. vector-source (addFeature current-point))
                                param (clj->js {:target dom-id
                                                :layers #js[(ol.layer.Tile. #js{:source (ol.source/OSM.)})
                                                            vector-layer]
                                                :view view
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
                                                        (swap! full-screen? not))))))
                        )))

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

(defn init []
  (let [app (js/document.getElementById "app")]
    (rdom/render [map-view] app)))
