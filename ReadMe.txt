Hi,

Allow me to give you a short introduction, before you experience the great joy of finding copy move forgeries.

If you don't have the time or patience to read, through this file. Consider taking a close look at the Tooltips. Most of the info can be found there as well.

0. Installing the Module Pack
-----------------------------

In Autopsy:

Tools > Plugins > Tab: Downloaded > Button: Add Plugins... > find your de-fau-copymoveforgerydetection.nbm > Open > Follow the instructions in the wizard.

de-fau-copymoveforgerydetection.nbm will most likely not be verified. 
If you don't want to install a module from a source that is not trusted, feel free to download the source code and build the .nbm file yourself! :)


1. Module Pack Contents
-----------------------

This module pack contains two modules:
	-CopyMoveForgeryDetectionIngest
	-CopyMoveForgeryDetectionContentViewer

1.1. CopyMoveForgeryDetectionIngest
-----------------------------------

Finds all Copy Move Forgeries within the images in the DataSource. For big images (i.e. 12 Megapixel) this process requires a lot of Memory (~ 2.3 GB per Thread started by Autopsy; 2 is default, please see your Autopsy options for further information) and a few minutes to calculate.
Please see Chapter 2 if you are unsure how to enable the JVM to use more memory.

The parallelize feature does require some additional Memory (~411 MB for our 12 Megapixel image per Core) and reduces the runtime only minimal (~20% with 4 cores for me). Please consider these numbers when deciding whether to activate this feature or not.
The other paramter RegionMinSize determines how many Regions with the same transformation between similar regions have to be found for a cluster of blocks to be considered a copy move forgery.
This basically translates to the minimum area in pixel that can be found as a copy move forgery.
-> The higher this number is chosen, the less noise (false positives) you will experience, but you might miss a smaller forgery by doing so.

In an optimization test with 97 images of typical size, 437 has been determined as a solid default value. Please feel free to adjust this number, if you are dealing with images of bigger/smaller size or with images that contain very small objects of interest.


1.2. CopyMoveForgeryDetectionContentViewer
------------------------------------------

Displays the image with the results of the copy move forgery detection. Blocks that have the same transformation are colored in the same color. Usually it is very easy to determine whether a result is in fact a detected forgery when comparing the original image with this one.
The viewer allows you to save the output image to your disk, in order to use it later on (i.e. in a report).


2. Increasing Java Heapsize in Windows
--------------------------------------

Control Panel > Java > Select Tab "Java" > Button "View" >

In your enabled version of java under "Runtime Parameters" add:
 -XmxMg (Replace "M" with the number of Gigabyte you want to enable for your JVM)
 or
 -XmxMm (Replace "M" with the number of Megabyte you want to enable for your JVM)


3. Algorithm Sources
--------------------

Fast Copy-Move Forgery Detection by HWEI-JEN LIN, CHUN-WEI WANG and YANG-TA KAO


4. Licence
------------------------


The CopyMove Module Package is distributed under the MIT Licence. Please see licence.txt for further information.

