# NMQC

Nuclear Medicine Quality Control utilities

Current capabilities

  1. Center of Rotation
    - Image must be a typical COR adquisition with point source
  2. Planar Uniformity
    - Handles octogonal, circular and rectangular shaped cameras
    - Automatically detects UFOV and CFOV
    - Calculate Integral and differential uniformity
  3. Pixel Size
    - Image must be a 2 lines phantom, you are expected to provide real distance between the lines
    - User must provide a ROI containing ONLY the relevant part of the image
  4. Spatial Resolution
    - Image must be a 2 lines phantom
    - User must provide a ROI containing ONLY the relevant part of the image
  5. Intrinsic Resolution and Linearity
    - Image must be a line grid phantom
    - User must provide a ROI containing ONLY the relevant part of the image
    - Limitation: the ROI cant include octogonal or circular shapes

