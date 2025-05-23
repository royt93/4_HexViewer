package com.galaxyjoy.hexviewer.ui.task;

import androidx.annotation.Nullable;

public interface TaskRunnerCallback<C, P, I, R> {

    /**
     * Runs on the UI thread.
     *
     * @param value The value indicating progress.
     */
    void onProgressUpdate(I value);

    /**
     * Called before the execution of the task.
     *
     * @return The Config.
     */
    @Nullable
    C onPreExecute();

    /**
     * Called after the execution of the task.
     *
     * @param result The result.
     */
    void onPostExecute(@Nullable final R result);

    /**
     * Performs a computation on a background thread.
     *
     * @param config The config of the task see {@link #onPreExecute}.
     * @param param  The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    @Nullable
    R doInBackground(@Nullable C config, @Nullable P param) throws TaskRunnerException;

    /**
     * Called when the async task is cancelled.
     */
    void onCancelled();

    /**
     * Called when the method {@link #doInBackground} raises an exception
     *
     * @param t The exception.
     */
    void onException(Throwable t);

}
