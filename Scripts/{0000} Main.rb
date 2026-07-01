loop do
    Graphics.update
    Input.update
    puts Graphics.frame_count
    if Graphics.frame_count == 1000
        Graphics.resize_screen(640, 480)
    end
end
