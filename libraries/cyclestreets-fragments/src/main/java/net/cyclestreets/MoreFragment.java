package net.cyclestreets;

import net.cyclestreets.fragments.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

public class MoreFragment extends Fragment implements View.OnClickListener
{
  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState)
  {
    final View view = inflater.inflate(R.layout.more, null);

    setButtonListener(view, R.id.settings_button);
    setButtonListener(view, R.id.blog_button);
    setButtonListener(view, R.id.about_button);

    final WebView donate = (WebView)view.findViewById(R.id.donate_view);
    donate.loadUrl("file:///android_asset/donate.html");

    return view;
  } // onCreate

  private void setButtonListener(final View view, final int id)
  {
    final Button b = (Button)view.findViewById(id);
    b.setOnClickListener(this);
  } // setButtonListener

  @Override
  public void onClick(View v)
  {
    final int viewId = v.getId();

    if(R.id.settings_button == viewId)
      start(SettingsActivity.class);
    if(R.id.blog_button == viewId)
      start(BlogActivity.class);
    if(R.id.about_button == viewId)
      start(AboutActivity.class);
  } // onClick

  private void start(final Class<? extends Activity> classToStart)
  {
    getActivity().startActivity(new Intent(getActivity(), classToStart));
  } // start
} // class MoreActivity
