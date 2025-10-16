package jp.mcapps.android.multi_payment_terminal;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import timber.log.Timber;

/*
    使ってるものと使いそうなものだけ抜粋
    必要なら随時追加
 */
public class NavigationWrapper {
    static final Application app = MainApplication.getInstance();

    public static void navigate(NavController navController, @IdRes int actionId, @Nullable Bundle args) {
        // いろいろ試したけどException握り潰すのが一番確実
        // バグがわかり辛くなるのでDebugビルドの時はcatchしないようにする？？
        try {
            navController.navigate(actionId, args);
        } catch (Exception e) {
            Timber.e("Navigation error!! actionId: %s", app.getResources().getResourceEntryName(actionId));
        }
    }

    /*** navigate by Activity ***/
    public static void navigate(Activity activity, @IdRes int viewId, @IdRes int actionId ,@Nullable Bundle args) {
        navigate(Navigation.findNavController(activity, viewId), actionId, args);
    }

    public static void navigate(Activity activity, @IdRes int viewId, @IdRes int actionId) {
        navigate(activity, viewId, actionId, null);
    }

    /*** navigate by View ***/
    public static void navigate(View view, @IdRes int actionId, @Nullable Bundle args) {
        navigate(Navigation.findNavController(view), actionId, args);
    }

    public static void navigate(View view, @IdRes int actionId) {
        navigate(view, actionId, null);
    }

    /*** navigate by Fragment ***/
    public static void navigate(Fragment fragment, @IdRes int actionId, @Nullable Bundle args) {
        navigate(NavHostFragment.findNavController(fragment), actionId, args);
    }

    public static void navigate(Fragment fragment, @IdRes int navigationId) {
        navigate(fragment, navigationId, null);
    }

    public static boolean popBackStack(NavController navController) {
        return navController.popBackStack();
    }

    /*** popBackStack by Activity ***/
    public static boolean popBackStack(Activity activity, @IdRes int containerId) {
        return popBackStack(Navigation.findNavController(activity, containerId));
    }

    /*** popBackStack by View ***/
    public static boolean popBackStack(View view) {
        return popBackStack(Navigation.findNavController(view));
    }

    /*** popBackStack by Fragment ***/
    public static boolean popBackStack(Fragment fragment) {
        return popBackStack(NavHostFragment.findNavController(fragment));
    }

    public static boolean navigateUp(NavController navController) {
        return navController.navigateUp();
    }

    /*** navigateUp by Activity ***/
    public static boolean navigateUp(Activity activity, @IdRes int viewId) {
        return navigateUp(Navigation.findNavController(activity, viewId));
    }

    /*** navigateUp by View ***/
    public static boolean navigateUp(View view) {
        return navigateUp(Navigation.findNavController(view));
    }

    /*** navigateUp by Fragment ***/
    public static boolean navigateUp(Fragment fragment) {
        return navigateUp(NavHostFragment.findNavController(fragment));
    }
}
