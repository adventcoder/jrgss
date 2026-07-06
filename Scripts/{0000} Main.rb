
Graphics.frame_rate = 30

loop do
    Graphics.update
    Input.update
    p(Input.pressed)
    if Input.trigger?(:C)
        if Input.press?(:SHIFT)
            raise 'oop!'
        else
            Graphics.resize_screen(640,480)
        end
    end
    sleep(rand / 60)
end
