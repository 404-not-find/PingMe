package com.ankurmittal.learning.adapters;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ankurmittal.learning.R;
import com.ankurmittal.learning.util.MD5Util;
import com.ankurmittal.learning.util.ParseConstants;
import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

public class FrndReqAdapter extends ArrayAdapter<ParseUser> {

	protected Context mContext;
	protected List<ParseUser> mUsers;
	protected List<ParseObject> mReqs;
	protected ParseUser currentUser;
	protected ProgressBar mProgressBar;

	public FrndReqAdapter(Context context, List<ParseUser> users,
			List<ParseObject> reqs, ProgressBar progBar) {
		super(context, R.layout.frnd_req_item, users);
		mContext = context;
		mUsers = users;
		mReqs = reqs;
		mProgressBar = progBar;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.frnd_req_item, null);
			holder = new ViewHolder();
			holder.userImageView = (ImageView) convertView
					.findViewById(R.id.frndReqSenderImageView);
			holder.nameLabel = (TextView) convertView
					.findViewById(R.id.frndReqSenderTextView);
			holder.mAcceptButton = (Button) convertView
					.findViewById(R.id.accept_frnd_req_btn);
			holder.mCancelButton = (Button) convertView
					.findViewById(R.id.cancel_frnd_req_btn);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		ParseUser user = mUsers.get(position);
		currentUser = ParseUser.getCurrentUser();
		String email = user.getEmail().toLowerCase();

		if (email.equals("")) {
			holder.userImageView.setImageResource(R.drawable.avatar_empty);
		} else {
			String hash = MD5Util.md5Hex(email);
			String gravatarUrl = "http://www.gravatar.com/avatar/" + hash
					+ "?s=204&d=404";
			Picasso.with(mContext).load(gravatarUrl)
					.placeholder(R.drawable.avatar_empty)
					.into(holder.userImageView);
		}

		holder.nameLabel.setText(user.getUsername());
		
		/////////if request is accepted/////////////
		holder.mAcceptButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("clicked", "accepted " + position);
				ParseRelation<ParseUser> frndRelation = currentUser
						.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
				frndRelation.add(mUsers.get(position));
				currentUser.saveInBackground(new SaveCallback() {

					@Override
					public void done(ParseException e) {
						if (e == null) {
							Toast.makeText(mContext, R.string.frnd_added,
									Toast.LENGTH_SHORT).show();
							// add current user as friend to sender
							ParseObject friendRequestAccepts = new ParseObject(
									ParseConstants.KEY_FRIENDS_REQUEST_ACCEPTS);
							friendRequestAccepts.put(
									ParseConstants.KEY_RECEIVER,
									mUsers.get(position).getObjectId());
							friendRequestAccepts.put(ParseConstants.KEY_REQUEST_SENDER,
									ParseUser.getCurrentUser());
							friendRequestAccepts.put(
									ParseConstants.KEY_SENDER_NAME, ParseUser
											.getCurrentUser().getUsername());
							friendRequestAccepts
									.saveInBackground(new SaveCallback() {

										@Override
										public void done(ParseException e) {

											if (e == null) {
												// friend request accept send
												Log.d("accept sent", "sent!");
											} else {
												// there was error
												Toast.makeText(
														mContext,
														R.string.friend_request_error_label,
														Toast.LENGTH_SHORT)
														.show();
											}

										}
									});
							//also remove the req form front end
							mUsers.remove(position);
							notifyDataSetChanged();
							// remove at backend
							mProgressBar.setVisibility(View.VISIBLE);
							ParseObject req = mReqs.get(position);
							req.deleteInBackground(new DeleteCallback() {

								@Override
								public void done(ParseException arg0) {
									mProgressBar.setVisibility(View.INVISIBLE);
									mReqs.remove(position);
									Toast.makeText(mContext, "deleted",
											Toast.LENGTH_SHORT).show();
									Log.d("checking", "" + mUsers.size());
								}
							});

						} else {
							// there was an error while adding frnd
							AlertDialog.Builder builder = new AlertDialog.Builder(
									mContext);
							builder.setTitle(R.string.error_title);
							builder.setMessage(e.getMessage());
							builder.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
							AlertDialog dialog = builder.create();
							dialog.show();
						}
					}
				});

			}
		});
		
		// ///////if cancel button is pressed/////////////
		holder.mCancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// remove from frontend
				mUsers.remove(position);
				Log.d("frndReqRemoved", "frndAdded" + position);
				notifyDataSetChanged();

				// remove at backend
				mProgressBar.setVisibility(View.VISIBLE);
				ParseObject req = mReqs.get(position);
				req.deleteInBackground(new DeleteCallback() {

					@Override
					public void done(ParseException arg0) {
						mProgressBar.setVisibility(View.INVISIBLE);
						mReqs.remove(position);
						Log.d("checking", "" + mUsers.size());
					}

				});

			}

		});

		return convertView;
	}

	private static class ViewHolder {
		ImageView userImageView;
		TextView nameLabel;
		Button mAcceptButton;
		Button mCancelButton;
		// TextView frndLabel;
	}

	public void refill(List<ParseUser> users) {
		mUsers.clear();
		mUsers.addAll(users);
		notifyDataSetChanged();
	}

}
