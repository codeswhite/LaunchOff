package eu.ottop.yamlauncher.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import eu.ottop.yamlauncher.settings.SharedPreferenceManager

class Animations(context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)
    var isInAnim = false

    private var transitionToken = 0

    // fadeViewIn and fadeViewOut are for smaller item transitions, such as the action menu
    fun fadeViewIn(view: View) {
        view.fadeIn()
    }

    fun fadeViewOut(view: View) {
        view.fadeOut()
    }

    fun showHome(homeView: View, appView: View, duration: Long) {
        transitionToken += 1
        val token = transitionToken

        cancelViewAnimation(homeView)
        cancelViewAnimation(appView)

        if (duration <= 0L) {
            appView.visibility = View.INVISIBLE
            appView.alpha = 0f
            appView.translationY = appView.height.toFloat() / 5
            appView.scaleY = 1.2f

            homeView.visibility = View.VISIBLE
            homeView.alpha = 1f
            homeView.translationY = 0f
            homeView.scaleY = 1f
            isInAnim = false
            return
        }

        isInAnim = true

        // Prepare deterministic start state
        homeView.visibility = View.VISIBLE
        homeView.alpha = if (homeView.alpha <= 0f) 0f else homeView.alpha
        homeView.translationY = if (homeView.translationY == 0f) -homeView.height.toFloat() / 100 else homeView.translationY

        appView.visibility = View.VISIBLE

        // Animate app out
        appView.animate()
            .translationY(appView.height.toFloat() / 5)
            .scaleY(1.2f)
            .alpha(0f)
            .setDuration(duration / 2)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (token != transitionToken) return
                    appView.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    if (token != transitionToken) return
                    appView.visibility = View.INVISIBLE
                }
            })

        // Animate home in
        homeView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleY(1f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (token != transitionToken) return
                    homeView.visibility = View.VISIBLE
                    homeView.alpha = 1f
                    homeView.translationY = 0f
                    isInAnim = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    if (token != transitionToken) return
                    homeView.visibility = View.VISIBLE
                    homeView.alpha = 1f
                    homeView.translationY = 0f
                    isInAnim = false
                }
            })
    }

    fun showApps(homeView: View, appView: View) {
        transitionToken += 1
        val token = transitionToken
        val duration = sharedPreferenceManager.getAnimationSpeed()

        cancelViewAnimation(homeView)
        cancelViewAnimation(appView)

        isInAnim = true

        // Prepare deterministic start state
        appView.visibility = View.VISIBLE
        appView.alpha = if (appView.alpha <= 0f) 0f else appView.alpha
        appView.translationY = if (appView.translationY == 0f) appView.height.toFloat() / 5 else appView.translationY
        appView.scaleY = if (appView.scaleY == 1f) 1.2f else appView.scaleY

        homeView.visibility = View.VISIBLE

        // Animate app in
        appView.animate()
            .translationY(0f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (token != transitionToken) return
                    appView.visibility = View.VISIBLE
                    appView.alpha = 1f
                    appView.translationY = 0f
                    appView.scaleY = 1f
                    isInAnim = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    if (token != transitionToken) return
                    appView.visibility = View.VISIBLE
                    appView.alpha = 1f
                    appView.translationY = 0f
                    appView.scaleY = 1f
                    isInAnim = false
                }
            })

        // Animate home out
        homeView.animate()
            .alpha(0f)
            .translationY(-homeView.height.toFloat() / 100)
            .setDuration(duration / 2)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (token != transitionToken) return
                    homeView.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    if (token != transitionToken) return
                    homeView.visibility = View.INVISIBLE
                }
            })
    }

    fun backgroundIn(activity: Activity) {
        val originalColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (originalColor == Color.parseColor("#00000000")) {
            val newColor = Color.parseColor("#3F000000")

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                activity.window.decorView.setBackgroundColor(animator.animatedValue as Int)
            }

            val duration = sharedPreferenceManager.getAnimationSpeed()
            backgroundColorAnimator.duration = duration

            backgroundColorAnimator.start()
        }
    }

    fun backgroundOut(activity: Activity, duration: Long) {
        val newColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (newColor == Color.parseColor("#00000000")) {
            val originalColor = Color.parseColor("#3F000000")

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                activity.window.decorView.setBackgroundColor(animator.animatedValue as Int)
            }

            backgroundColorAnimator.duration = duration

            backgroundColorAnimator.start()
        }
    }

    private fun View.fadeIn(duration: Long = sharedPreferenceManager.getAnimationSpeed()) {
        animate().cancel()
        alpha = 0f
        translationY = -height.toFloat() / 100
        visibility = View.VISIBLE

        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setListener(null)
    }

    private fun View.fadeOut() {
        animate().cancel()
        if (visibility == View.VISIBLE || alpha > 0f) {
            val duration = sharedPreferenceManager.getAnimationSpeed()
            animate()
                .alpha(0f)
                .translationY(-height.toFloat() / 100)
                .setDuration(duration / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility = View.INVISIBLE
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        visibility = View.INVISIBLE
                    }
                })
        }
    }

    private fun cancelViewAnimation(view: View) {
        view.animate().setListener(null)
        view.animate().cancel()
        view.clearAnimation()
    }
}
