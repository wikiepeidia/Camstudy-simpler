# Removed unused drawable resources

The following drawable assets were deleted after verifying that there were no `@drawable/...` or `R.drawable...` references to them anywhere under `app/src/main`:

- `arrow_back_24px`
- `arrow_forward_24px`
- `cameraswitch_24px`
- `delete_24px`
- `flash_on_24px`
- `flip_camera_ios_24px`
- `gallery`
- `grid`
- `history_24px`
- `ic_camera_24`
- `ic_delete`
- `ic_grid`
- `ic_image`
- `ic_zoom_thumb`
- `image_24px`
- `photo_camera_24px`
- `photo_camera_back_24px`
- `save_as_24px`
- `trash`

This cleanup was identified using ripgrep searches for direct resource usages.
