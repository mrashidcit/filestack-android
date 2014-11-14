package io.filepicker.api;

import android.content.Context;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import io.filepicker.Filepicker;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.PreferencesUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class FpApiClient {

    public static final String DIALOG_URL = "dialog.filepicker.io";
    public static final String DIALOG_ENDPOINT = "https://" + DIALOG_URL;
    public static final String API_PATH_URL = "/api/path/";
    public static final String API_CLIENT_URL = "/api/client/";
    public static final String API_PATH_COMPUTER_URL = "/api/path/computer";
    public static final String AUTH_OPEN_URL = "/auth/open";


    private static FpApiInterface fpApiInterface;

    public static FpApiInterface getFpApiClient( Context context ) {
        if (fpApiInterface == null) {
            setFpApiClient(context);
        }
        return fpApiInterface;
    }

    public static void setFpApiClient(Context context) {
        final PreferencesUtils prefs = PreferencesUtils.newInstance(context);
        RestAdapter restAdapter = null;

        if (prefs.getSessionCookie() != null) {
            // Build with cookie
            restAdapter = getCookieRestAdapter(prefs.getSessionCookie());
        } else {
            // Build without cookie
            restAdapter = getRestAdapter();
        }

        fpApiInterface = restAdapter.create(FpApiInterface.class);
    }

    public static RestAdapter getRestAdapter() {
        return new RestAdapter.Builder()
                .setEndpoint(DIALOG_ENDPOINT)
                .build();
    }

    public static RestAdapter getCookieRestAdapter(final String session) {
        return new RestAdapter.Builder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Cookie", "session=" + session);
                    }
                })
                .setEndpoint(DIALOG_ENDPOINT)
                .build();
    }

    public interface FpApiInterface {
        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{provider}")
        void getFolder(
                @Path("provider") String provider,
                @Query("format") String format,
                @Query("js_session") String jsSession,
                Callback<Folder> folder
                );

        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{filePath}")
        FPFile pickFile(
                @Path("filePath") String filePath,
                @Query("format") String format,
                @Query("js_session") String jsSession
        );

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_COMPUTER_URL)
        void uploadFile(
                @Header("X-File-Name") String filename,
                @Query("js_session") String jsSession,
                @Body TypedFile file,
                Callback<UploadLocalFileResponse> response
        );

        @Headers("User-Agent: Mobile-Android")
        @GET(API_CLIENT_URL + "{provider}" + "/unauth")
        void logout(
                @Path("provider") String provider,
                @Query("js_session") String jsSession,
                Callback<Object> fpFile
        );

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_URL + "{path}")
        void exportFile(@Path("path") String path,
                        @Query("js_session") String jsSession,
                        @Body TypedFile file,
                        Callback<FPFile> response);
    }

    public static String getJsSession(Context context) {
        return buildJsSession(Filepicker.getApiKey(),
                PreferencesUtils.newInstance(context).getMimetypes());
    }

    /** JsSession query param  */
    public static String buildJsSession(String apikey, String[] mimetypes) {
        Gson gson = new Gson();

        if(mimetypes == null) {
            return gson.toJson(new JsSession(apikey));
        } else {
            return gson.toJson(new MimetypeSession(apikey, mimetypes));
        }
    }

    /** JsSession query param class */
    static class JsSession {
        HashMap<String, String> app;
        String mimetypes;

        JsSession(String apikey) {
            this.app = new HashMap<String, String>();
            this.app.put("apikey", apikey);
            this.mimetypes = "";
        }
    }

    static class MimetypeSession  {
        HashMap<String, String> app;
        String[] mimetypes;

        MimetypeSession(String apikey, String[] mimetypes) {
            this.app = new HashMap<String, String>();
            this.app.put("apikey", apikey);
            this.mimetypes = mimetypes;
        }
    }
}