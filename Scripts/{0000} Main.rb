
Audio.bgm_play('Audio/BGM/Airship', 10, 150, 5000000)

loop do
    Graphics.update
    Input.update
    puts(Audio.bgm_pos)
    if Input.trigger?(:B)
        msgbox("hoy")
    elsif Input.trigger?(:C)
        if Input.press?(:SHIFT)
            raise 'oops!'
        else
            if Graphics.width == 640
                Graphics.resize_screen(544, 416)
            else
                Graphics.resize_screen(640, 480)
            end
        end
    end
end
