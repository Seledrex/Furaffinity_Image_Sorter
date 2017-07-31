# Furaffinity Image Sorter

A Java application for automatically sorting images downloaded from Furaffinity.

## Specifications

The Furaffinity Image Sorter will only sort content downloaded directly from Furaffinity, and only if the content's filename was not manually modified.

Furaffinity's naming convention follows this format:
```
[10 digit number].[username]_[original filename].[extension]
Example: 1495007059.ashdarkfire_ор.png.jpg
```

Furaffinity Image Sorter can sort multiple folders at once. The sorting algorithm; however, will not recursively check directories for images. The recommended way to sort images is to place all images inside one folder before running the sort. The Furaffinity Image Sorter will create a new directory for each artist/user found from the filename, only if the artist's directory does not exist. It will also skip copying content that already exists inside the output folder.

## Instructions

1. Download [Furaffinity Image Sorter.jar](https://github.com/Seledrex/Furaffinity_Image_Sorter/blob/master/Furaffinity%20Image%20Sorter.jar)
2. Run the executable jar file
3. Add the input folders
4. Set the output folder
5. Sort the images

## TODO

- Add loading progress bars/circle things
- Create recursive file searching for input folders
- Download and sort all submissions/favorites feature
- Download and sort all artwork from a user's gallery feature

![alt tag](https://raw.githubusercontent.com/Seledrex/Furaffinity_Image_Sorter/master/Furaffinity%20Image%20Sorter.png)
