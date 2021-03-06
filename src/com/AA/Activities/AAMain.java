/*Copyright 2010 University Of Utah Android Development Group
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package com.AA.Activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.AA.R;
import com.AA.Other.Article;
import com.AA.Other.DisplayTypes;
import com.AA.Recievers.AlarmReceiver;
import com.AA.Services.RssService;

/***
 * This is the main activity of the app...it is what is launched 
 * when the user starts the application
 */
public class AAMain extends ListActivity {
	private final int OPEN = 0;
	private final int SHARE = 1;
	private final int MARK = 2;

	//***GUI Member Variables go here***
	ImageButton ib_refresh;
	//***End GUI Member Variables***

	ArticleAdapter adapter;
	SharedPreferences settings;

	BroadcastReceiver finishReceiver;

	List<Article> articles;

	View selectedView;

	ProgressDialog progressDialog;

	/***
	 * Called when the activity is created and put into memory.
	 * 
	 * This is where all GUI elements should be set up and any
	 * other member variables that is used throughout the class
	 */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		articles = new ArrayList<Article>();

		//Creates access to the application settings and marks the access code as
		//closed off to just our app
		settings = this.getSharedPreferences("settings", 0);
		AlarmReceiver.stopAlarm(this);

		//This sets up our adapter to use and tells our activity to use it to fill itself
		adapter = new ArticleAdapter(this);
		this.setListAdapter(adapter);

		//***GUI Elements Set up here***
		ib_refresh = (ImageButton) findViewById(R.id.ib_refresh);

		//Sets custom font for app title.
		TextView tv=(TextView)findViewById(R.id.AATitle);
		Typeface face=Typeface.createFromAsset(getAssets(), "fonts/WREXHAM_.TTF");
		tv.setTypeface(face);
		//***End GUI Set up***

		/**Catches when the service has finished downloading the RSS**/
		finishReceiver = new BroadcastReceiver() {
			@Override public void onReceive(Context context,
						  Intent intent) {
				articles.clear();

				Bundle articleBundle = intent.getBundleExtra("articles");
				ArrayList<String> titles = articleBundle.getStringArrayList("titles");

				for(String title : titles)
					articles.add((Article)articleBundle.getSerializable(title));

				progressDialog.cancel();

				refresh();
		}};

		//Registers the Receiver with this activity
		this.registerReceiver(finishReceiver,
					  new IntentFilter("RSSFinish"));

		//***Action Listeners set up here***
		ib_refresh.setOnClickListener(new OnClickListener() {
			/***
			 * Handles when the user clicks the refresh button
			 * @param v - view that was clicked
			 */
			@Override public void onClick(View v) {
				runService();
			}
			});
			//***End Action Listener set up***

		//Starts the service
		runService();
	}

	/***
	 * Called when another activity takes over the foreground.
	 * Also called when the the screen goes off or when the screen
	 * is rotated. 
	 *
	 * Save any data that may be floating around at the moment, here
	 * ***CORRECTION - Save your data in the onSaveInstanceState(), not here.
	 */
	@Override protected void onPause() {
		//This cancels the receiver(requirement on the Android Dev Guide)
		this.unregisterReceiver(finishReceiver);
		RssService.writeData(this, articles);

		//If there is widgets that require background data fetching, start the alarm up
		//once the activity is finished
		if(settings.getInt("widgetCount", 0) > 0)
			AlarmReceiver.startAlarm(this);
		super.onPause();
	}

	/***
	 * Called when the activity comes back into the foreground
	 *
	 * Restore your data here(to give the user a seamless experience)
	 */
	@Override protected void onResume() {
		//Registers the Receiver with this activity
		this.registerReceiver(finishReceiver,
					  new IntentFilter("RSSFinish"));
		this.refresh();
		super.onResume();
	}

	/***
	 * Refreshes the article list. Should start the RSS service
	 * and then refreshes the data in the main activity list using
	 * the ArticleAdapter
	 */
	private void refresh() {
		adapter.clear();
		Collections.sort(articles);
		adapter.addList(articles);
	}

	/***
	 * Runs the fetching service. Pops up with a progress dialog, so that the user
	 * knows something is happening in the background.
	 */
	private void runService() {
		//Creates and shows a progress dialog
		progressDialog = ProgressDialog.show(this, "", "Loading News. Please Wait...");
		progressDialog.setCancelable(true);
		
		//This thread class will start the service in the background so that
		//we aren't interrupting the application process
		Intent service = new Intent();
		service.putExtra("background", false);
		service.setClass(AAMain.this, RssService.class);
		AAMain.this.startService(service);
	}

	/***
	 * Creates the ContextMenu that shows up when the user presses MENU
	 * 
	 * Should display "Settings" when the user presses MENU
	 */
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(getString(R.string.settings));
		return true;
	}

	/***
	 * Starts the settings activity when user presses "Settings"
	 * 
	 * @param menuItem - Item selected from the options menu
	 */
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Intent activity = new Intent();
		if (item.getTitle().equals(getString(R.string.settings))) {
			activity.setClass(this, AASettings.class);
			this.startActivity(activity);
			return true;
		} else
			return false;
	}

	/***
	 * Creates a list of options when long pressing an item that has been registered for a
	 * context menu
	 */
	@Override public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
		selectedView = v;
		Article article = (Article) v.getTag();

		//Creates the menu items
		menu.add(ContextMenu.NONE, OPEN, 0, "Open");
		menu.add(ContextMenu.NONE, SHARE, 1, "Share");

		if (article.isRead())
			menu.add(ContextMenu.NONE, MARK, 1, "Mark as unread");
		else
			menu.add(ContextMenu.NONE, MARK, 1, "Mark as read");
	}

	/***
	 * Handles when the user selects an option in the ContextMenu
	 */
	@Override public boolean onContextItemSelected(MenuItem item) {
		Article a = (Article) selectedView.getTag();

		if (item.getItemId() == OPEN)
			openBrowser(a);
		else if (item.getItemId() == SHARE)
			shareDialog(a);
		else if (item.getItemId() == MARK)
			a.toggleRead();

		//Tells the adapter to refresh itself
		adapter.notifyDataSetChanged();
		return super.onContextItemSelected(item);
	}

	/***
	 * Opens the browser at the given URL
	 * @param url - URL that we want opened in the browser
	 */
	private void openBrowser(Article a) {
		Intent browserLaunch = new Intent();
		a.markRead();

		//Sets this intent to launch the default browser app with the given URL
		browserLaunch.setAction(Intent.ACTION_DEFAULT);
		browserLaunch.addCategory(Intent.CATEGORY_BROWSABLE);
		browserLaunch.setData(Uri.parse(a.getUrl()));
		this.startActivity(browserLaunch);
	}

	/***
	 * Opens a dialog of possible places to share the article
	 *
	 * Should hopefully allow for email, SMS, Facebook, and Twitter
	 *
	 * @param a - Article to share
	 */
	private void shareDialog(Article a) {
		Intent shareChooser = new Intent(Intent.ACTION_SEND);

		shareChooser.setType("text/plain");

		//Puts a subject in our article and some text from the article
		shareChooser.putExtra(Intent.EXTRA_SUBJECT,
					  "Check this article out");
		shareChooser.putExtra(Intent.EXTRA_TEXT,
					  a.getTitle() +
					  " from Absolutely Android\n" +
					  a.getDescription() + "\n" +
					  "To read more, click this link(or copy it into URL bar): "
					  + a.getUrl());

		startActivity(Intent.createChooser(shareChooser,
						"How do you want to share?"));
	}

	/***
	 * This adapter will take the article data and format each
	 * row of a list. This data includes the title, date, and the
	 * article description
	 *
	 * @author Tyler Robinson 
	 *
	 * (Everyone else who edit this file should add their name)
	 */
	private class ArticleAdapter extends ArrayAdapter < Article > {
		/***
		 * Constructor - An array adapter has several different constructors.
		 * This one required both a list of articles and a layout resource for
		 * each row.
		 *
		 * @param context - Context that will be using this adapter
		 * @param resource - Layout resource that will define the design of each row
		 * @param textViewResourceId - Usually used for simple text view lists...not really needed since we have the row layout
		 * @param objects - List of articles that we will display in the list
		 */
		public ArticleAdapter(Context context) {
			super(context, R.layout.article_layout, R.id.iv_title);
		}
		/***
		 * Adds a list of items into the list view
		 *
		 * @param articles - that are being added to our list view of articles
		 */
		public void addList(List < Article > articles) {
			  for (Article article:articles)
				this.add(article);
		}

		/***
		 * Called when the row is in the users current view. Rows should be prepared here
		 *
		 * It is necessary to inflate the row that was given in the constructor, before you
		 * are able to change individual pieces of each row...I have code for this if we need it.
		 *
		 * @param position - Current position in the list that is being prepared to be displayed
		 * @param convertView - Old view that needs to be converted...we won't use this.
		 * @param parent - parent that this view gets attached to
		 */
		@Override public View getView(int position, View convertView,
					ViewGroup parent) {
			//Creates a layout inflater using the main activity's context
			LayoutInflater inflater =
				AAMain.this.getLayoutInflater();
			SharedPreferences settings = AAMain.this.settings;

			View row;

			//Parses the layout we want into a View, so that we can access each
			//individual piece if we haven't already(in which we just use convertView)
			if (convertView == null)
				row = inflater.inflate(R.layout.article_layout, null);
			else
				row = convertView;

			//Grabs our TextViews from our article row layout for edit
			TextView tv_title =
				(TextView) row.findViewById(R.id.tv_title);
			TextView tv_date =
				(TextView) row.findViewById(R.id.tv_date);
			TextView tv_description =
				(TextView) row.findViewById(R.id.
							tv_description);

			//Gets the article that will be displayed at this position
			Article article = this.getItem(position);

			//Puts our data into each of the TextViews for the user's view pleasure
			tv_title.setText(article.getTitle());
			tv_date.setText(article.getDate());
			tv_description.setText(article.getDescription());

			//Grabs our background color for the read/unread from the settings and sets
			//the row background to reflect that					
			int bgColor;
			int textColor;

			if (article.isRead()) {
				bgColor = settings.getInt(DisplayTypes.colorRead.toString(), Color.WHITE);
				textColor = settings.getInt(DisplayTypes.txtRead.toString(), Color.BLACK);
			}else{
				bgColor = settings.getInt(DisplayTypes.colorUnread.toString(), Color.BLACK);
				textColor = settings.getInt(DisplayTypes.txtUnread.toString(), Color.WHITE);
			}
			row.setBackgroundColor(bgColor);

			//Produces a complementary color of the background color and sets
			//it to the foreground color; this way the user never hides the text
			tv_title.setTextColor(textColor);
			tv_description.setTextColor(textColor);
			tv_date.setTextColor(textColor);

			//Allows for long pressing a row item
			AAMain.this.registerForContextMenu(row);

			//Stores the article within the view(for access elsewhere)
			row.setTag(article);

			/**Click listener for the row**/
			row.setOnClickListener(new OnClickListener() {
			/***
			 * Open the file browser when user clicks the article
			 */
			@Override public void onClick(View v){
				AAMain.this.openBrowser((Article) v.getTag());
				ArticleAdapter.this.notifyDataSetChanged();}
			});

			return row;
		}
	}
}
