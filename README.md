# UIUC Toilet App
A simple app built in Android Studio in Java, with support for API 16+ (Android version 4.0.3+). Created by me for fun.

## Demo
Checkout the app on the Google Play store here.
image

## Features
* MapView using Google Maps SDK to display markers based on user current location with information about the map.
* Add bathrooms into the database at the current user location.
* RecyclerView list with the closest 20 bathrooms to the users current location, updating.
* Options to customize the nearby list by hiding bathrooms, or creating a new favorites list.

## Notes
Information is stored using a REST API into a MongoDB database. No user information is collected, simply the resources needed to display bathroom information. API is hosted on heroku at: https://uiuc-toilet.herokuapp.com

## Installation
a.) Install the app through the .apk in release (make sure to allow installation from unknown sources).

b.) Clone this repo, build the gradle in Android Studio and run the app through an AVD (virtual device).

