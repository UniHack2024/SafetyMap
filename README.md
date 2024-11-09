
# Safety Map Android App

This repository contains the source code for a safety alert mobile app, designed to help users stay aware of potential safety hazards in their vicinity. The app allows users to view, create, and receive alerts on an interactive map regarding safety issues and civic problems, such as stolen bikes, falling plaster, or potholes in the road. The app also sends notifications for nearby hazards and automatically submits civic complaints to the local town hall.

## Project Overview

The Safety Alert Android App is a community-driven platform that aims to:

- Notify users of potential hazards in their area.
- Allow users to mark and categorize various safety alerts on an interactive map.
- Send push notifications to users entering hazardous areas.
- Automate the process of sending complaints about civic issues to local authorities.

## Features

- **Interactive Map Interface**: The app features a Google Maps-based interactive map, where users can add warnings about various hazards and see alerts from others.
- **User Alerts**: Users can drop pins on the map to report issues such as thefts, road hazards, or other dangers.
- **Notifications**: Real-time notifications for users approaching an area with a safety alert.
- **Civic Complaints**: Automatically submit complaints about civic issues (e.g., potholes, falling plaster) to the local town hall.
- **Community Trust System**: Users can vote on the validity of alerts, contributing to a community trust score.

## Technical Stack

- **Front-end**: Java (Android)
- **Back-end**: Python (Django/Flask)
- **Map Integration**: Google Maps API
- **Database**: PostgreSQL
- **Notification Service**: Firebase Cloud Messaging (FCM)

## Project Setup

### Prerequisites

- **Java Development Kit (JDK)**: Install JDK 8 or above.
- **Android Studio**: Install Android Studio for building and testing the mobile application.
- **Python Environment**: Set up Python 3 with either Django or Flask.
- **PostgreSQL**: Install PostgreSQL for database management.
- **Firebase Account**: Create a Firebase project and set up FCM for push notifications.

### Getting Started

1. **Clone the Repository**
   ```sh
   git clone https://github.com/UniHack2024/project.git
   cd project
   ```

2. **Backend Setup**
   - Navigate to the `backend` directory.
   - Create a virtual environment and install the required dependencies:
     ```sh
     python -m venv venv
     source venv/bin/activate  # On Windows use `venv\Scripts\activate`
     pip install -r requirements.txt
     ```
   - Set up PostgreSQL and update the database connection details in the `settings.py` file (for Django) or in the Flask configuration.
   - Run the server:
     ```sh
     python manage.py runserver  # For Django
     ```

3. **Android App Setup**
   - Open the `android-app` directory in Android Studio.
   - Connect your Firebase project to the Android app for FCM integration.
   - Update the Google Maps API key in `res/values/google_maps_api.xml`.
   - Build and run the app on an emulator or a physical device.

### Database Setup

- Set up a PostgreSQL instance and create a database named `safety_alert_db`.
- Run the migrations to create the necessary tables:
  ```sh
  python manage.py migrate
  ```

### Environment Variables

- **Google Maps API Key**: Set up an environment variable or update the configuration file with your Google Maps API key.
- **Firebase Configuration**: Add the Firebase configuration file (`google-services.json`) to the `android-app` directory.

## Contributing

We welcome contributions! Please follow the standard GitHub fork-and-pull request workflow:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Create a pull request for review.

## Contact

For questions or suggestions, please open an issue or contact the project maintainer at [brigadainginerilor@gmail.com].
