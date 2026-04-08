# 2024-Atra
ATRA (Another Tool for Running/Route Analysis) is an online tool that helps users analyze the data from their running activities so that they can get insights from their data. It will also have a social aspect, allowing users to create groups called Murals. In these groups, users could compare their times to friend's times, set group goals, or compare routes.

This project is my (Ángel Marqués) TFG. As such, it is guided by a professor from King Juan Carlos University, and follows a specific format. This format is divided into 5 phases, and as such, so will this readme file.

# Table of contents
1. [Phase 0](#phase-0)
	1. [Project info](#project-info)
	2. [Functionalities](#functionalities)
	   * [Basic Functionalities](#basic-functionalities)
	   * [Advanced Functionalities](#advanced-functionalities)
	1. [Entities](#entities)
	2. [Permissions](#user-permissions)
	3. [Images](#images)
	4. [Graphs](#graphs)
	5. [Complimentary Technologies](#complimentary-technologies)
	6. [Algorithm or Advanced Query](#algorithm-or-advanced-query)
2. [Phase 1](#phase-1-feature-definition-and-initial-designs)
3. [Phase 2](#phase-2-feature-implementation-by-entity)
4. [Phase 3](#phase-3-quality-assurance)
5. [Phase 4](#phase-4-dockerization-cicd-and-deployment)
6. [Phase 5](#phase-5-documentation-writing)
7. [Phase 6](#phase-6-presentation-preparation)

# Phase 0
This is the planning phase. It records information about the project, like its entities and functionalities.


## Project info
App Name: ATRA (Another Tool for Running/Route Analysis)
Student: Ángel Marqués García
Tutor: Micael Gallego Carrillo
Cotutor: Rubén Morante González

Blog: 
GitHub Project: https://github.com/orgs/codeurjc-students/projects/14

## Entities
Actions preceded by (~) are optional/potential. They are actions that COULD be implemented, however, they might not be neccessary or helpful. As such, they are recorded, so that they are considered, but if they are not implemented, they will be deleted from this table.
| Entity   | Description                                                                                                                                                        | Created when                                                                                                                                     | Deleted when                                                                                               | Actions                                                                                                                                                                                                       | Related to                                                                                                                                                                                                                             |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| User     | Holds all the information about a specific user                                                                                                                    | An anonymous user signs up                                                                                                                       | A user deletes their account                                                                               | - delete user <br> -edit profile                                                                                                                                                                              | -Mural <br>-Activity <br>-(optional) Route                                                                                                                                                                                             |
| Activity | Holds information about a given activity. The gpx holding its data, as well as metrics derived from that for quicker access.                                       | A user uploads a GPX or a new Activity is detected in Strava                                                                                     | The user it belongs to chooses to do so                                                                    | -Delete<br>-Visualize data<br>-Analyze data<br>-Add to Route<br>-Change visibility<br>-(advanced) Compare data                                                                                                | -(optional) Route: the route it takes place in <br>- (optional) User: the user who owns it                                                                                                                                             |
| Route    | Holds information about a route.                                                                                                                                   | A user chooses to do so, taking an activity as a base<br>(advanced) When the algorithm recognizes a new route<br>When importing data from Strava | It no longer has any activities<br>A user (its creator) chooses to do so (unsure that this is a good idea) | -View activities<br>-(\~) Add activity (from a list of activities with no route. I would rather giving a route an activity be handled from the activity's side)<br><br>-(\~) Remove activity (same as before) | -Activity: list of the activities on this route<br>-(optional) User: list of users with at least one activity on this route                                                                                                            |
| Mural    | Holds a list of users, and is used to show the data from all of them. Users can join with its id, and they can decide what activities they want a Mural to access. | A user chooses to do so                                                                                                                          | Its owner chooses to do so                                                                                 | -Join<br>-Leave<br>-(advanced) Kick user<br>-View data (activities and analysis)                                                                                                                              | -User: list of Users that are part of the Mural<br>-(optional) Activity: list of visible activities (to make access quicker)<br>-(optional) Route: list of Routes such that at least one of the activities owned happens on that route |

## Functionalities
Functionalities will be divided into two groups: Basic and Advanced. Basic functionalities form part of the MVP, and will be implemented in Phase 1. Advanced functionalities are improvements or additional tools. They are not strictly neccessary for the core of the app, but are still good to have. They are further divided into "Must have" and "Optional" functionalities. "Must have" functionalities are considered essential for the app to have.  They add important functionalities that the app should have before release. "Optional" functionalities add additional functionalities or quality of life improvements, however those functionalities and improvements are not as essential for the app. As such, some might not be implemented by the time this project is completed. 

### Basic Functionalities
| Entity   | Functionality                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------------------------- |
| User     | -Login<br>-Logout<br>-Delete account<br>-Edit account<br>-Set Profile Picture<br>-Sign up                                   |
| Mural    | -Create Mural<br>-Delete Mural<br>-Join Mural<br>-See Mural page                                                            |
| Activity | -Upload Activity (GPX)<br>-Sync with Strava<br>-View Activity data<br>-Analyze Activity data<br>-Change activity visibility |
| Route    | -Create Route<br>-Assign Route to Activity<br>-Link image to Route<br>-Visualize and Analyze cumulative data                
### Advanced Functionalities
| Entity   | Must have                                                                                                                               | Optional                                                                                                                                             |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| User     |                                                                                                                                         | -Access full functionality as anonymous<br>-Download session<br>-Customize home page/dashboard<br>-Download infographic                              |
| Mural    | - Invite user to Mural<br>- Own Mural<br>- Kick user from owned Mural<br>- Compare activities in Mural<br>- Only Owner can delete Mural | - Highlight when users reach a goal/break a record<br>- Endorse other's activities (give medals or similar)                                          |
| Activity | - Compare 2 activities in the same route<br>- Compare an activity with its route avg                                                    | - Automatically assign Route<br>- Compare two activities on different Routes <br> - Upload activity (TCX)                                            |
| Route    |                                                                                                                                         | - Automatically create Route from Activities<br>- Automatically identify Segments<br>- Show a 3D map<br>- Add an autorunner (strava's is super cool) |
## User permissions
The following table lists user permissions as they should be in the completed app. However, since accessing full functionality as an anonymous user is considered an advanced functionality, it might not be implemented, and anonymous users would only be able to log in.

| Action                           | Anonymous | Registered | Admin |
| -------------------------------- | --------- | ---------- | ----- |
| Analyze Activities               | yes       | yes        | yes   |
| Compare Activities               | yes       | yes        | yes   |
| Create Route                     | yes       | yes        | yes   |
| Join Mural                       | no        | yes        | yes   |
| Create Mural                     | no        | yes        | yes   |
| Activities are saved in database | no        | yes        | yes   |


## Images
The app will be able to handle images uploaded by users, and potentially also generate images for users to download.
- Users can upload a profile picture
- Routes can have an image linked
- (advanced) Users can download an infographic summarizing their training and achievements

## Graphs
Graphs are essential to the app. After all, it is a tool to help users analyze and visualize the data from their running activities, and graphs are a great tool for that. The app will use many different types of graphs. Some examples are below:
- Line graphs (pace/time)
- Bar graphs (avg pace/week)
- Histograms (% of time in each heartrate zone)
- Scatter plots (distribution of pace readings)
- Pie charts (% of total distance contributed by each member in a Mural)
- Rainfall diagrams and similar (pace distribution/activity)
- And potentially others
## Complimentary technologies
The format of the TFG requires the use of some type of complimentary technology, like accessing an API or similar. This will be achieved through the following:
- A mapping API will be used to show the route taken
- The app will connect to the Strava API to request a user's activities
- (Optional) The app could send emails to users when reaching specific goals
## Algorithm or advanced query

The format of the TFG requires the use of some advanced algorithm or query. Given the nature of the project, it is not hard to implement some type of algorithm. For example:
- Some metrics could be obtained by applying specific algorithms (GAP)
- Analyzing activities will make use of various algorithms
- Comparing activities will make use of various algorithms
- Algorithms could be used to automatically identify and assign Routes to Activities
- Algorithms could be used to automatically detect and create segments within a Route/Activity
- A recommendation algorithm could be implemented to suggest similar routes to ones you've participated in.

## Wireframe
Lastly, what follows is a sketch showing the design of the main windows the users will navigate, as well as indications on how to traverse through them, and some notes indicating how the app is expected to behave.



# Phase 1: Feature definition and initial designs
A list of functionalities required for the application to deliver the desired experience was created. These functionalities were classified into basic, essential advanced, and optional advanced. Additionally, they were grouped according to the main entity they operate on. After defining the features, mockups were created to outline the different screens and the layout of elements within them.

# Phase 2: Feature implementation by entity
The defined functionalities were developed by implementing all basic features of a given entity before moving on to another. Once all basic features were completed, a second iteration focused on implementing essential and optional advanced features.

# Phase 3: Quality assurance
Automated tests were developed, and analyses were performed using SonarQube to identify and address vulnerabilities, as well as maintainability and complexity issues.

# Phase 4: Dockerization, CI/CD, and deployment
The Azure environment was configured to enable deployment, along with the CI/CD workflows responsible for it, and the necessary Docker configurations for packaging the application.

# Phase 5: Documentation writing
The report for the project was written to capture the entire development process of the project.

# Phase 6: Presentation preparation
A PowerPoint presentation was created, along with other necessary preparations for the project defense before a committee.

---
# Report
The redacted report can be found in this repository under /docs/thesis/tfg_2024_Atra.pdf. The source code used to generate the pdf can be found under /docs/thesis/latex. In order to generate the pdf yourself, you'll need to compress the files in this folder, and upload them to overleaf or some other LaTex editor, then download the built pdf from there. 
