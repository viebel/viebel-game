(ns viebel-game.core
	(:use-macros [purnam.core :only [! !>]]
	           [cljs.core.async.macros :only [go go-loop]]
	           [gyr.core :only [def.module 
                             	def.controller
                              	def.directive]])
	(:require [cljs.core.async :refer [<! chan put!]]))

(enable-console-print!)



(def.module app [ngTouch ngAnimate])






(def user-input-chan (chan))

(def item-val (atom nil))
(def item-count (atom 0))
(def item-score (atom 0))
(def item-timer (atom 0))

(def status-running (atom false))

(defn on-item-val-change [$scope _ _ _ new-value]
	(.$apply $scope (! $scope.item.val new-value)))

(defn on-item-count-change [$scope _ _ _ new-value]
	(.$apply $scope (! $scope.item.count new-value)))

(defn on-item-score-change [$scope _ _ _ new-value]
	(.$apply $scope (! $scope.item.score new-value)))

(defn on-item-timer-change [$scope _ _ _ new-value]
	(! $scope.item.timer new-value))

(defn on-status-running-change [$scope _ _ _ new-value]
	(.$apply $scope (! $scope.status.running new-value)))

(defn init-watchers [$scope]
	(add-watch item-val :on-item-val-change (partial on-item-val-change $scope))
 	(add-watch item-count :on-item-count-change (partial on-item-count-change $scope))
  	(add-watch item-score :on-item-score-change (partial on-item-score-change $scope))
    (add-watch item-timer :on-item-timer-change (partial on-item-timer-change $scope))
    (add-watch status-running :on-status-running-change (partial on-status-running-change $scope))
	(!> $scope.$on "$destroy" (fn[]
	                            (remove-watch item-val :on-item-val-change)
                             	(remove-watch item-count :on-item-count-change)
                              	(remove-watch item-score :on-item-score-change)
                               	(remove-watch item-timer :on-item-score-change)
                                (remove-watch status-running :on-status-running-change))))

(defn init-game [] 
   	(reset! item-timer 30))

(defn timeout [$timeout]
  (let [c (chan)]
    ($timeout #(put! c :tic) 1000)
    c))

(defn start-timer [$timeout]
	(go-loop [timer 30]
        (reset! item-timer timer)
		(<! (timeout $timeout))
		(if (= timer 0)
			(put! user-input-chan :end)
			(recur (dec timer)))))
  
(defn run-game [$timeout]
  (go-loop [item-past nil]
      (reset! item-val (rand-nth [2 4 6]))
      (case (<! user-input-chan)
        :start (do
                 (start-timer $timeout)
                 (reset! status-running true)
                 (reset! item-count 0)
                 (reset! item-score 0)
                 (recur @item-val))
        :end (do
               (reset! status-running false)
               (reset! item-val "END"))
        :yes (do
               (if (= item-past @item-val) (swap! item-score inc))
               (swap! item-count inc)
               (recur @item-val))
        :no (do
              (if (not= item-past @item-val) (swap! item-score inc))
              (swap! item-count inc)
              (recur @item-val)))))

(defn init-model [$scope $timeout]
  	(init-game)
	(run-game $timeout))

(defn init-handlers [$scope]
  (! $scope.game #(put! user-input-chan :start))
  (! $scope.yes #(when @status-running (put! user-input-chan :yes)))
  (! $scope.no #(when @status-running (put! user-input-chan :no))))

(def.controller app.MainCtrl [$scope $timeout]
  (init-watchers $scope)
  (init-model $scope $timeout)
  (init-handlers $scope))





(defn animate-item [$animate element]
   	(.addClass $animate element "wobble" #(.removeClass $animate element "wobble")))

(def.directive app.animateOnChange [$animate]
  (fn [$scope element attrs]
    (.$watch $scope attrs.animateOnChange #(animate-item $animate element))))