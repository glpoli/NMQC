# Nuclear Medicine Quality Control utilities

Plugins for performing quality controls in nuclear medicine images, current version is still unstable and requires validation.

Released under Apache License version 2, see License.

## Current capabilities

  1. Centre of Rotation
    - Image must be a typical COR acquisition with point source
  2. Planar Uniformity
    - Handles octagonal, circular and rectangular shaped cameras
    - Automatically detects UFOV and CFOV
    - Calculate Integral and differential uniformity
  3. Spatial Resolution and Pixel Size
    - Image must be a 2 lines phantom (User made)
    - User must provide a ROI containing ONLY the relevant part of the image
  4. Four Bar Phantom
    - Image must be a Four bar phantom (User made) acquisition
	- Automatically detects a ROI containing the whole phantom and centred on it
  5. Quadrant Bar Phantom
    - Image must be a Quadrant Bar (NEMA) acquisition
    - Automatically detect all four quadrants and bar sizes 
    - Sorts quadrants in ascending bar order	
  6. Intrinsic Resolution and Linearity
    - Image must be a line grid phantom (NEMA)
	- Automatically detects UFOV and CFOV
  7. Tomographic Contrast
    - Image must be a 3D reconstruction from an Jasczak Phantom acquisition
	- Two forms of proceeding: Automatic (user shall only provide a number referring a uniformity region frame) and Manual (The user shall mark with PointRois the positions of the spheres and provide a number referring a uniformity region frame)
	- The contrast is calculated using the minimum/maximum value in the sphere in case there are cold/hot sphere phantom respectively
	- In the Automatic method it detects phantom border and returns the positions of every sphere (even artefacts!), user shall discriminate which detection is correct
	- In the Manual method it detects phantom border and returns the positions of every sphere in the neighbouring of selected points 
	
## Work in Progress

  1. Tomographic Uniformity
    - The procedure is not clear in the bibliography, however we implemented rings contrast
	
# Tools

  1. Geometric Mean
    - Useful to get the geometric mean in renal studies for antero-posterior views
	- Two images (Anterior and Posterior views) are expected
	- Anterior image is mirrored for alignment
  2. Cardiac Reslicer
    - Useful to separate the scatter from the emission views in cardiac studies
	- Expected an image with a sequence of scatter - emission images in all segments
	- Images can be gated

