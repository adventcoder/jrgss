loop do
    Graphics.update
    Input.update
    if Input.trigger?(:C)
        Graphics.resize_screen(640,480)
    end
end
