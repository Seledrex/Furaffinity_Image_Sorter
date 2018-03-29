# Furaffinity Image Sorter V1.0

A Java application for downloading and sorting images from Furaffinity.

## Specifications

Furaffinity Image Sorter is a simple application for downloading and sorting images from Furaffinity. When running the application, you will be asked to set your 'Stash' which is the folder that will contain your collection of artwork. It is recommended to set your stash to an empty folder when first starting to use the application. After your stash is set, you may either import artwork into your stash using the 'Import artwork' and 'Sort artwork' buttons, or you may download artwork directly from Furaffinity by pressing the 'Download artwork' button. However, in order to download artwork, you must first login to your Furaffinity account using the application (internet connection is required). Simply press the 'Login' button, then type in your corresponding credentials for your Furaffinity account. Passwords will not be saved. Once you are logged in, the application will keep you logged in for subsequent sessions until you press the 'Logout' button. After pressing the 'Download artwork' button, a dialog window will appear. To download artwork, type in the desired username of whom you wish to download artwork from, and then press the 'Favorites', 'Gallery', or 'Scraps' button below to download that user's corresponding Favorites, Gallery, or Scraps. Depending on how many pages of artwork there in a user's favorites, gallery, or scraps, the download may take longer to initialize. After starting a download, the download may be stopped at any time by pressing the 'Stop' button. Furaffinity Image Sorter will only download artwork not contained within your stash in order to avoid having duplicates and to optimize download time. The artwork will automatically be sorted and placed in your stash. If you ever wish to change your stash, press the 'Set stash' button, and this will allow you to select a new folder to be your stash.

The Furaffinity Image Sorter will only download and sort content directly from Furaffinity, and only if the content's filename was not manually modified.

Furaffinity's naming convention follows this format:
```
[number].[username]_[original filename].[extension]
Example: 1495007059.ashdarkfire_ор.png.jpg
```

Furaffinity Image Sorter can sort multiple folders at once. The sorting algorithm; however, will not recursively check folders for images. The recommended way to sort images is to place all images inside one folder before running the sort. The Furaffinity Image Sorter will create a new directory for each artist/user found from the filename, only if the artist's directory does not exist. It will also skip copying content that already exists inside the output folder. If a file cannot be sorted, it will be copied into the 'unsorted' folder within the stash.

The application takes advantage of browser cookies and property files. The browser cookies will be saved as 'cookie.file' and the user properties will be saved in 'user.properties'. In addition, the application takes advantage of multithreading capabilities, and will try to download multiple images at a time to speed up download speed. A strong internet connection is needed to download artwork quickly.

## Instructions

1. Download and install [Java](https://java.com/en/download/) on your computer
2. Download [Furaffinity Image Sorter.zip](https://github.com/Seledrex/Furaffinity_Image_Sorter/blob/master/Furaffinity%20Image%20Sorter.zip)
3. Extract the folder
4. Run the executable JAR file inside the folder
5. Set your stash (can be the given stash folder or anywher else of your choosing)
6. Dowload and sort images as much as you like!

## TODO

- Download all submissions feature

## Screenshots

![alt tag](https://raw.githubusercontent.com/Seledrex/Furaffinity_Image_Sorter/master/main.PNG)
![alt tag](https://raw.githubusercontent.com/Seledrex/Furaffinity_Image_Sorter/master/login_dialog.png)
![alt tag](https://raw.githubusercontent.com/Seledrex/Furaffinity_Image_Sorter/master/download_dialog.PNG)
